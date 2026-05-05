import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AgendaDia, Bloqueio, BloqueioPayload, Espaco, EspacoTipo } from '../../core/models';
import { ApiErrorService } from '../../core/services/api-error.service';
import { BloqueiosService } from '../../core/services/bloqueios.service';
import { EspacosService } from '../../core/services/espacos.service';
import { ReservasService } from '../../core/services/reservas.service';

type AgendaSlotStatus = 'LIVRE' | 'RESERVADO' | 'BLOQUEADO';

interface AgendaSlot {
  index: number;
  inicio: string;
  fim: string;
  status: AgendaSlotStatus;
  detalhe: string;
}

interface PendingDeleteState {
  bloqueio: Bloqueio;
  removerSerie: boolean;
}

const SLOT_DURATION_MINUTES = 60;
const DEFAULT_OPENING_MINUTES = 6 * 60;
const DEFAULT_CLOSING_MINUTES = 23 * 60;

@Component({
  selector: 'app-bloqueios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
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

  readonly form = this.fb.nonNullable.group({
    espacoId: [0, [Validators.required, Validators.min(1)]],
    data: [this.today(), [Validators.required]],
    motivo: ['', [Validators.maxLength(300)]],
    recorrenteSemanal: [false],
    dataFimRecorrencia: [this.addWeeks(this.today(), 4)]
  });

  espacos: Espaco[] = [];
  bloqueios: Bloqueio[] = [];
  agenda: AgendaDia | null = null;
  agendaSlots: AgendaSlot[] = [];
  selectedSlotIndexes: number[] = [];
  pendingDelete: PendingDeleteState | null = null;

  loadingEspacos = true;
  loadingAgenda = false;
  saving = false;
  deleting = false;
  readonly minDate = this.today();

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
          this.clearSelectedSlots();
          this.loadAgenda();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao criar bloqueio.');
        }
      });
  }

  toggleSlotSelection(slot: AgendaSlot): void {
    if (slot.status !== 'LIVRE') {
      return;
    }

    const selected = [...this.selectedSlotIndexes].sort((a, b) => a - b);
    const alreadySelected = selected.includes(slot.index);

    if (alreadySelected) {
      if (selected.length === 1) {
        this.clearSelectedSlots();
        return;
      }

      if (slot.index === selected[0]) {
        this.selectedSlotIndexes = selected.slice(1);
        return;
      }

      if (slot.index === selected[selected.length - 1]) {
        this.selectedSlotIndexes = selected.slice(0, -1);
        return;
      }

      this.selectedSlotIndexes = [slot.index];
      return;
    }

    if (selected.length === 0) {
      this.selectedSlotIndexes = [slot.index];
      return;
    }

    const first = selected[0];
    const last = selected[selected.length - 1];

    if (slot.index === first - 1) {
      this.selectedSlotIndexes = [slot.index, ...selected];
      return;
    }

    if (slot.index === last + 1) {
      this.selectedSlotIndexes = [...selected, slot.index];
      return;
    }

    this.selectedSlotIndexes = [slot.index];
  }

  isSlotSelected(slot: AgendaSlot): boolean {
    return this.selectedSlotIndexes.includes(slot.index);
  }

  slotStatusLabel(slot: AgendaSlot): string {
    if (this.isSlotSelected(slot)) {
      return 'Selecionado';
    }

    if (slot.status === 'LIVRE') {
      return 'Livre';
    }

    if (slot.status === 'BLOQUEADO') {
      return 'Bloqueado';
    }

    return 'Reservado';
  }

  requestRemoveBloqueio(bloqueio: Bloqueio, removerSerie = false): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.pendingDelete = { bloqueio, removerSerie };
  }

  cancelRemoveBloqueio(): void {
    if (this.deleting) {
      return;
    }

    this.pendingDelete = null;
  }

  confirmRemoveBloqueio(): void {
    const pendingDelete = this.pendingDelete;
    if (!pendingDelete) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';
    this.deleting = true;
    this.bloqueiosService
      .delete(pendingDelete.bloqueio.id, pendingDelete.removerSerie)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.successMessage = pendingDelete.removerSerie
            ? 'Série de bloqueios removida com sucesso.'
            : 'Bloqueio removido com sucesso.';
          this.pendingDelete = null;
          this.loadAgenda();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao remover bloqueio.');
        }
      });
  }

  get selectedEspaco(): Espaco | undefined {
    return this.espacos.find((espaco) => espaco.id === this.form.controls.espacoId.value);
  }

  get selectedEspacoTipoLabel(): string {
    return this.selectedEspaco ? this.tipoLabel(this.selectedEspaco.tipo) : '';
  }

  get selectedEspacoHorarioLabel(): string {
    if (!this.selectedEspaco) {
      return '';
    }

    return `${this.normalizeTime(this.selectedEspaco.horarioFuncionamentoInicio)} às ${this.normalizeTime(this.selectedEspaco.horarioFuncionamentoFim)}`;
  }

  get selectedSlots(): AgendaSlot[] {
    return this.agendaSlots.filter((slot) => this.selectedSlotIndexes.includes(slot.index));
  }

  get selectedIntervalLabel(): string {
    if (this.selectedSlots.length === 0) {
      return 'Nenhum horário selecionado';
    }

    const primeiraFaixa = this.selectedSlots[0];
    const ultimaFaixa = this.selectedSlots[this.selectedSlots.length - 1];
    return `${primeiraFaixa.inicio} às ${ultimaFaixa.fim}`;
  }

  get canCreateBlock(): boolean {
    return !this.saving && this.form.valid && this.selectedSlots.length > 0 && (!this.isRecurringEnabled || !!this.form.controls.dataFimRecorrencia.value);
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
    return tipo === 'QUADRA' ? 'Quadra' : 'Quiosque';
  }

  isRecurringBlock(bloqueio: Bloqueio): boolean {
    return !!bloqueio.serieRecorrenciaId;
  }

  private observeFilters(): void {
    this.form.controls.espacoId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.clearSelectedSlots();
        this.loadAgenda();
      });

    this.form.controls.data.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => {
        this.clearSelectedSlots();

        if (this.isRecurringEnabled) {
          const dataFimAtual = this.form.controls.dataFimRecorrencia.value;
          if (!dataFimAtual || dataFimAtual < data) {
            this.form.controls.dataFimRecorrencia.setValue(this.addWeeks(data, 4), { emitEvent: false });
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
            this.form.controls.dataFimRecorrencia.setValue(this.addWeeks(dataInicio, 4), { emitEvent: false });
          }
          return;
        }

        this.form.controls.dataFimRecorrencia.setValue(this.addWeeks(this.form.controls.data.value, 4), { emitEvent: false });
      });
  }

  private loadEspacos(): void {
    this.loadingEspacos = true;
    this.espacosService
      .list()
      .pipe(finalize(() => (this.loadingEspacos = false)))
      .subscribe({
        next: (espacos) => {
          this.espacos = espacos.sort((a, b) => a.nome.localeCompare(b.nome));

          if (this.espacos.length > 0 && this.form.controls.espacoId.value === 0) {
            this.form.patchValue({ espacoId: this.espacos[0].id });
            return;
          }

          this.loadAgenda();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar espaços.');
        }
      });
  }

  private loadAgenda(): void {
    const espacoId = this.form.controls.espacoId.value;
    const data = this.form.controls.data.value;

    this.pendingDelete = null;
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
          this.agendaSlots = this.buildAgendaSlots(agenda);
        },
        error: (error: unknown) => {
          this.agenda = null;
          this.agendaSlots = [];
          this.bloqueios = [];
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar a agenda do dia.');
        }
      });
  }

  private buildAgendaSlots(agenda: AgendaDia): AgendaSlot[] {
    const slots: AgendaSlot[] = [];
    const startMinutes = this.resolveOpeningMinutes(this.selectedEspaco?.horarioFuncionamentoInicio, DEFAULT_OPENING_MINUTES);
    const endMinutes = this.resolveOpeningMinutes(this.selectedEspaco?.horarioFuncionamentoFim, DEFAULT_CLOSING_MINUTES);

    let index = 0;
    for (let start = startMinutes; start + SLOT_DURATION_MINUTES <= endMinutes; start += SLOT_DURATION_MINUTES) {
      const end = start + SLOT_DURATION_MINUTES;
      const inicio = this.minutesToTime(start);
      const fim = this.minutesToTime(end);
      const bloqueio = agenda.bloqueios.find((item) => this.hasOverlap(start, end, item.horarioInicio, item.horarioFim));
      const reserva = agenda.reservasAtivas.find((item) => this.hasOverlap(start, end, item.horarioInicio, item.horarioFim));

      if (bloqueio) {
        slots.push({
          index,
          inicio,
          fim,
          status: 'BLOQUEADO',
          detalhe: bloqueio.motivo?.trim() || ''
        });
        index += 1;
        continue;
      }

      if (reserva) {
        slots.push({
          index,
          inicio,
          fim,
          status: 'RESERVADO',
          detalhe: ''
        });
        index += 1;
        continue;
      }

      slots.push({
        index,
        inicio,
        fim,
        status: 'LIVRE',
        detalhe: ''
      });
      index += 1;
    }

    return slots;
  }

  private clearSelectedSlots(): void {
    this.selectedSlotIndexes = [];
  }

  private hasOverlap(slotStart: number, slotEnd: number, start: string, end: string): boolean {
    const faixaInicio = this.timeToMinutes(start);
    const faixaFim = this.timeToMinutes(end);
    return slotStart < faixaFim && slotEnd > faixaInicio;
  }

  private today(): string {
    const now = new Date();
    const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
    return localDate.toISOString().split('T')[0];
  }

  private addWeeks(dateValue: string, weeks: number): string {
    const date = new Date(`${dateValue}T00:00:00`);
    date.setDate(date.getDate() + weeks * 7);
    return date.toISOString().split('T')[0];
  }

  private normalizeTime(value: string | null | undefined): string {
    return (value ?? '').slice(0, 5);
  }

  private resolveOpeningMinutes(value: string | null | undefined, fallback: number): number {
    const normalized = this.normalizeTime(value);
    if (!normalized) {
      return fallback;
    }

    return this.timeToMinutes(normalized);
  }

  private timeToMinutes(value: string): number {
    const [hours, minutes] = value.split(':').map((part) => Number(part));
    return hours * 60 + minutes;
  }

  private minutesToTime(value: number): string {
    const hours = Math.floor(value / 60)
      .toString()
      .padStart(2, '0');
    const minutes = (value % 60).toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  }
}
