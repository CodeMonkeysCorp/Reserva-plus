import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AgendaDia, Bloqueio, BloqueioPayload, BloqueioRotina, Espaco, EspacoTipo } from '../../core/models';
import { formatEspacoTipoLabel } from '../../core/espaco-tipo';
import { ApiErrorService } from '../../core/services/api-error.service';
import { BloqueiosService } from '../../core/services/bloqueios.service';
import { EspacosService } from '../../core/services/espacos.service';
import { ReservasService } from '../../core/services/reservas.service';
import { DateFieldComponent } from '../../shared/ui/date-field/date-field.component';
import { SelectFieldComponent } from '../../shared/ui/select-field/select-field.component';
import { addWeeksToDateInput, currentDateInputValue } from '../../shared/utils/date-input.utils';
import {
  AgendaSlot,
  buildAgendaSlots,
  buildSelectedIntervalLabel,
  formatTimeRange,
  slotStatusLabel as getAgendaSlotStatusLabel,
  toggleContiguousSlotSelection
} from '../../shared/utils/agenda-slot.utils';

interface DeleteActionState {
  bloqueioId: number;
  removerSerie: boolean;
}

const SLOT_DURATION_MINUTES = 60;
const DEFAULT_OPENING_MINUTES = 6 * 60;
const DEFAULT_CLOSING_MINUTES = 23 * 60;

@Component({
  selector: 'app-bloqueios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SelectFieldComponent, DateFieldComponent],
  templateUrl: './bloqueios.component.html',
  styleUrl: './bloqueios.component.css'
})
export class BloqueiosComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly bloqueiosService = inject(BloqueiosService);
  private readonly espacosService = inject(EspacosService);
  private readonly reservasService = inject(ReservasService);
  private readonly apiErrorService = inject(ApiErrorService);
  private readonly weekdayFormatter = new Intl.DateTimeFormat('pt-BR', { weekday: 'long' });

  readonly form = this.fb.nonNullable.group({
    espacoId: [0, [Validators.required, Validators.min(1)]],
    data: [currentDateInputValue(), [Validators.required]],
    motivo: ['', [Validators.maxLength(300)]],
    recorrenteSemanal: [false],
    dataFimRecorrencia: [addWeeksToDateInput(currentDateInputValue(), 4)]
  });

  espacos: Espaco[] = [];
  bloqueios: Bloqueio[] = [];
  rotinas: BloqueioRotina[] = [];
  agenda: AgendaDia | null = null;
  agendaSlots: AgendaSlot[] = [];
  selectedSlotIndexes: number[] = [];
  deletingAction: DeleteActionState | null = null;

  loadingEspacos = true;
  loadingAgenda = false;
  loadingRotinas = false;
  saving = false;
  readonly minDate = currentDateInputValue();

  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.observeFilters();
    this.loadEspacos();
  }

  criarBloqueio(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.selectedSlots.length === 0) {
      this.errorMessage = 'Selecione pelo menos um horário livre para criar o bloqueio.';
      return;
    }

    const primeiraFaixa = this.selectedSlots[0];
    const ultimaFaixa = this.selectedSlots[this.selectedSlots.length - 1];
    const recorrenteSemanal = this.form.controls.recorrenteSemanal.value;

    const payload: BloqueioPayload = {
      espacoId: this.form.controls.espacoId.value,
      data: this.form.controls.data.value,
      horarioInicio: primeiraFaixa.inicio,
      horarioFim: ultimaFaixa.fim,
      motivo: this.form.controls.motivo.value.trim() || undefined,
      recorrenteSemanal,
      dataFimRecorrencia: recorrenteSemanal ? this.form.controls.dataFimRecorrencia.value : undefined
    };

    this.saving = true;
    this.bloqueiosService
      .create(payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.successMessage = recorrenteSemanal
            ? `Série semanal criada com sucesso para ${this.recurrenceOccurrencesCount} ocorrência(s).`
            : 'Bloqueio criado com sucesso.';
          this.resetSelectionState();
          this.loadAgenda();
          this.loadRotinas();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao criar bloqueio.');
        }
      });
  }

  toggleSlotSelection(slot: AgendaSlot): void {
    this.selectedSlotIndexes = toggleContiguousSlotSelection(this.selectedSlotIndexes, slot);
  }

  isSlotSelected(slot: AgendaSlot): boolean {
    return this.selectedSlotIndexes.includes(slot.index);
  }

  slotStatusLabel(slot: AgendaSlot): string {
    return getAgendaSlotStatusLabel(slot.status, this.isSlotSelected(slot));
  }

  removeBloqueio(bloqueio: Bloqueio, removerSerie = false): void {
    if (this.deletingAction) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';
    this.deletingAction = { bloqueioId: bloqueio.id, removerSerie };
    this.bloqueiosService
      .delete(bloqueio.id, removerSerie)
      .pipe(finalize(() => (this.deletingAction = null)))
      .subscribe({
        next: () => {
          this.successMessage = removerSerie
            ? 'A rotina semanal foi removida com sucesso.'
            : 'Bloqueio removido com sucesso.';
          this.loadAgenda();
          this.loadRotinas();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao remover bloqueio.');
        }
      });
  }

  removeRotina(rotina: BloqueioRotina): void {
    if (this.deletingAction) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';
    this.deletingAction = { bloqueioId: rotina.id, removerSerie: true };
    this.bloqueiosService
      .delete(rotina.id, true)
      .pipe(finalize(() => (this.deletingAction = null)))
      .subscribe({
        next: () => {
          this.successMessage = 'A rotina semanal foi removida com sucesso.';
          this.loadAgenda();
          this.loadRotinas();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao remover rotina.');
        }
      });
  }

  isDeleting(bloqueio: Bloqueio, removerSerie = false): boolean {
    return this.deletingAction?.bloqueioId === bloqueio.id && this.deletingAction.removerSerie === removerSerie;
  }

  isDeletingRotina(rotina: BloqueioRotina): boolean {
    return this.deletingAction?.bloqueioId === rotina.id && this.deletingAction.removerSerie;
  }

  get deleting(): boolean {
    return this.deletingAction !== null;
  }

  get selectedEspaco(): Espaco | undefined {
    return this.espacos.find((espaco) => espaco.id === this.form.controls.espacoId.value);
  }

  get espacoOptions(): Array<{ value: number; label: string }> {
    return this.espacos.map((espaco) => ({
      value: espaco.id,
      label: `${espaco.nome} (${this.tipoLabel(espaco.tipo)})`
    }));
  }

  get selectedEspacoTipoLabel(): string {
    return this.selectedEspaco ? this.tipoLabel(this.selectedEspaco.tipo) : '';
  }

  get selectedEspacoHorarioLabel(): string {
    if (!this.selectedEspaco) {
      return '';
    }

    return formatTimeRange(
      this.selectedEspaco.horarioFuncionamentoInicio,
      this.selectedEspaco.horarioFuncionamentoFim
    );
  }

  get selectedSlots(): AgendaSlot[] {
    return this.agendaSlots.filter((slot) => this.selectedSlotIndexes.includes(slot.index));
  }

  get selectedIntervalLabel(): string {
    return buildSelectedIntervalLabel(this.selectedSlots);
  }

  get canCreateBlock(): boolean {
    return !this.saving
      && this.form.valid
      && this.selectedSlots.length > 0
      && (!this.isRecurringEnabled || !!this.form.controls.dataFimRecorrencia.value);
  }

  get motivoLength(): number {
    return this.form.controls.motivo.value.length;
  }

  get isRecurringEnabled(): boolean {
    return this.form.controls.recorrenteSemanal.value;
  }

  get minRecurrenceEndDate(): string {
    return this.form.controls.data.value || this.minDate;
  }

  get recurrenceOccurrencesCount(): number {
    if (!this.isRecurringEnabled) {
      return 1;
    }

    const dataInicio = this.form.controls.data.value;
    const dataFim = this.form.controls.dataFimRecorrencia.value;
    if (!dataInicio || !dataFim) {
      return 0;
    }

    const start = new Date(`${dataInicio}T00:00:00`);
    const end = new Date(`${dataFim}T00:00:00`);
    if (end < start) {
      return 0;
    }

    const diffDays = Math.floor((end.getTime() - start.getTime()) / (24 * 60 * 60 * 1000));
    return Math.floor(diffDays / 7) + 1;
  }

  tipoLabel(tipo: EspacoTipo): string {
    return formatEspacoTipoLabel(tipo);
  }

  isRecurringBlock(bloqueio: Bloqueio): boolean {
    return !!bloqueio.serieRecorrenciaId;
  }

  recurrenceDayLabel(dateValue: string): string {
    const label = this.weekdayFormatter.format(new Date(`${dateValue}T12:00:00`));
    return label.charAt(0).toUpperCase() + label.slice(1);
  }

  private observeFilters(): void {
    this.form.controls.espacoId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.resetSelectionState();
        this.loadAgenda();
        this.loadRotinas();
      });

    this.form.controls.data.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => {
        this.resetSelectionState();

        if (this.isRecurringEnabled) {
          const dataFimAtual = this.form.controls.dataFimRecorrencia.value;
          if (!dataFimAtual || dataFimAtual < data) {
            this.form.controls.dataFimRecorrencia.setValue(addWeeksToDateInput(data, 4), { emitEvent: false });
          }
        }

        this.loadAgenda();
      });

    this.form.controls.recorrenteSemanal.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((enabled) => {
        if (enabled) {
          const dataInicio = this.form.controls.data.value;
          const dataFimAtual = this.form.controls.dataFimRecorrencia.value;
          if (!dataFimAtual || dataFimAtual < dataInicio) {
            this.form.controls.dataFimRecorrencia.setValue(addWeeksToDateInput(dataInicio, 4), { emitEvent: false });
          }
          return;
        }

        this.form.controls.dataFimRecorrencia.setValue(
          addWeeksToDateInput(this.form.controls.data.value, 4),
          { emitEvent: false }
        );
      });
  }

  private loadEspacos(): void {
    this.loadingEspacos = true;
    this.espacosService
      .list()
      .pipe(finalize(() => (this.loadingEspacos = false)))
      .subscribe({
        next: (espacos) => {
          this.espacos = [...espacos].sort((a, b) => a.nome.localeCompare(b.nome));

          if (this.espacos.length > 0 && this.form.controls.espacoId.value === 0) {
            this.form.patchValue({ espacoId: this.espacos[0].id });
            return;
          }

          this.loadAgenda();
          this.loadRotinas();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar espaços.');
        }
      });
  }

  private loadRotinas(): void {
    const espacoId = this.form.controls.espacoId.value;

    this.rotinas = [];

    if (espacoId <= 0) {
      return;
    }

    this.loadingRotinas = true;
    this.bloqueiosService
      .listRecurring(espacoId)
      .pipe(finalize(() => (this.loadingRotinas = false)))
      .subscribe({
        next: (rotinas) => {
          this.rotinas = [...rotinas].sort((a, b) => {
            const compareData = a.dataInicio.localeCompare(b.dataInicio);
            return compareData !== 0 ? compareData : a.horarioInicio.localeCompare(b.horarioInicio);
          });
        },
        error: (error: unknown) => {
          this.rotinas = [];
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar rotinas semanais.');
        }
      });
  }

  private loadAgenda(): void {
    const espacoId = this.form.controls.espacoId.value;
    const data = this.form.controls.data.value;

    this.agenda = null;
    this.agendaSlots = [];
    this.bloqueios = [];

    if (espacoId <= 0 || !data) {
      return;
    }

    this.loadingAgenda = true;
    this.reservasService
      .agenda(espacoId, data)
      .pipe(finalize(() => (this.loadingAgenda = false)))
      .subscribe({
        next: (agenda) => {
          this.agenda = agenda;
          this.bloqueios = [...agenda.bloqueios].sort((a, b) => a.horarioInicio.localeCompare(b.horarioInicio));
          this.agendaSlots = buildAgendaSlots(agenda, {
            openingTime: this.selectedEspaco?.horarioFuncionamentoInicio,
            closingTime: this.selectedEspaco?.horarioFuncionamentoFim,
            fallbackOpeningMinutes: DEFAULT_OPENING_MINUTES,
            fallbackClosingMinutes: DEFAULT_CLOSING_MINUTES,
            slotDurationMinutes: SLOT_DURATION_MINUTES,
            isPastSlot: (_slotStartMinutes, slotEndMinutes) => this.isPastSlot(slotEndMinutes)
          });
        },
        error: (error: unknown) => {
          this.agenda = null;
          this.agendaSlots = [];
          this.bloqueios = [];
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar a agenda do dia.');
        }
      });
  }

  private clearSelectedSlots(): void {
    this.selectedSlotIndexes = [];
  }

  private resetSelectionState(): void {
    this.clearSelectedSlots();
  }

  private isPastSlot(endMinutes: number): boolean {
    const selectedDate = this.form.controls.data.value;
    if (!selectedDate || selectedDate !== currentDateInputValue()) {
      return false;
    }

    return endMinutes <= this.currentMinutes();
  }

  private currentMinutes(): number {
    const now = new Date();
    return now.getHours() * 60 + now.getMinutes();
  }
}
