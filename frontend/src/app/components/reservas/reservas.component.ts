import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';
import { AgendaDia, Espaco, EspacoTipo, ReservaCreatePayload } from '../../core/models';
import { collectEspacoTipos, formatEspacoTipoLabel } from '../../core/espaco-tipo';
import { ApiErrorService } from '../../core/services/api-error.service';
import { AuthService } from '../../core/services/auth.service';
import { EspacosService } from '../../core/services/espacos.service';
import { ReservasService } from '../../core/services/reservas.service';
import { DateFieldComponent } from '../../shared/ui/date-field/date-field.component';
import { SelectFieldComponent } from '../../shared/ui/select-field/select-field.component';
import { addDaysToCurrentDate, currentDateInputValue } from '../../shared/utils/date-input.utils';
import {
  AgendaSlot,
  buildAgendaSlots,
  buildSelectedIntervalLabel,
  formatTimeRange,
  slotStatusLabel as getAgendaSlotStatusLabel,
  toggleContiguousSlotSelection
} from '../../shared/utils/agenda-slot.utils';

type FiltroTipoEspaco = 'TODOS' | EspacoTipo;

const SLOT_DURATION_MINUTES = 60;
const DEFAULT_OPENING_MINUTES = 6 * 60;
const DEFAULT_CLOSING_MINUTES = 23 * 60;
const RESERVA_MAX_DAYS_AHEAD = 7;
const BOOKING_DATE_RANGE_ERROR = 'bookingDateRange';

function bookingDateRangeValidator(maxDaysAhead: number): ValidatorFn {
  return (control: AbstractControl<string | null>): ValidationErrors | null => {
    const value = control.value;
    if (!value) {
      return null;
    }

    const minDate = currentDateInputValue();
    const maxDate = addDaysToCurrentDate(maxDaysAhead);
    if (value < minDate || value > maxDate) {
      return { [BOOKING_DATE_RANGE_ERROR]: true };
    }

    return null;
  };
}

@Component({
  selector: 'app-reservas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SelectFieldComponent, DateFieldComponent],
  templateUrl: './reservas.component.html',
  styleUrl: './reservas.component.css'
})
export class ReservasComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly reservasService = inject(ReservasService);
  private readonly espacosService = inject(EspacosService);
  private readonly authService = inject(AuthService);
  private readonly apiErrorService = inject(ApiErrorService);

  readonly maxBookingWindowDays = RESERVA_MAX_DAYS_AHEAD;
  readonly form = this.fb.nonNullable.group({
    tipo: ['TODOS' as FiltroTipoEspaco, [Validators.required]],
    espacoId: [0, [Validators.required, Validators.min(1)]],
    data: [currentDateInputValue(), [Validators.required, bookingDateRangeValidator(RESERVA_MAX_DAYS_AHEAD)]]
  });

  readonly isAdmin = this.authService.isAdmin();

  espacos: Espaco[] = [];
  agenda: AgendaDia | null = null;
  agendaSlots: AgendaSlot[] = [];
  selectedSlotIndexes: number[] = [];

  loading = true;
  loadingAgenda = false;
  creating = false;

  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.observeFilters();
    this.loadEspacos();
  }

  criarReserva(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.form.controls.data.hasError(BOOKING_DATE_RANGE_ERROR)) {
        this.errorMessage = `Selecione uma data entre hoje e os próximos ${this.maxBookingWindowDays} dias.`;
        return;
      }

      this.errorMessage = 'Selecione o tipo, o espaço e a data da reserva.';
      return;
    }

    if (this.selectedSlots.length === 0) {
      this.errorMessage = 'Selecione pelo menos um horário livre para continuar.';
      return;
    }

    const primeiraFaixa = this.selectedSlots[0];
    const ultimaFaixa = this.selectedSlots[this.selectedSlots.length - 1];

    const payload: ReservaCreatePayload = {
      espacoId: this.form.controls.espacoId.value,
      data: this.form.controls.data.value,
      horarioInicio: primeiraFaixa.inicio,
      horarioFim: ultimaFaixa.fim
    };

    this.creating = true;

    this.reservasService
      .create(payload)
      .pipe(finalize(() => (this.creating = false)))
      .subscribe({
        next: () => {
          this.successMessage = 'Reserva realizada com sucesso.';
          this.clearSelectedSlots();
          this.loadAgenda();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao criar reserva.');
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

  tipoLabel(tipo: EspacoTipo): string {
    return formatEspacoTipoLabel(tipo);
  }

  get filteredEspacos(): Espaco[] {
    const tipo = this.form.controls.tipo.value;
    if (tipo === 'TODOS') {
      return this.espacos;
    }

    return this.espacos.filter((espaco) => espaco.tipo === tipo);
  }

  get hasFilteredEspacos(): boolean {
    return this.filteredEspacos.length > 0;
  }

  get tipoOptions(): Array<{ value: FiltroTipoEspaco; label: string }> {
    const selectedTipo = this.form.controls.tipo.value;
    const tipos = collectEspacoTipos([
      ...this.espacos.map((espaco) => espaco.tipo),
      selectedTipo === 'TODOS' ? null : selectedTipo
    ]);

    return [
      { value: 'TODOS', label: 'Todos os tipos' },
      ...tipos.map((tipo) => ({
        value: tipo,
        label: this.tipoLabel(tipo)
      }))
    ];
  }

  get espacoOptions(): Array<{ value: number; label: string; disabled?: boolean }> {
    if (!this.hasFilteredEspacos) {
      return [];
    }

    return this.filteredEspacos.map((espaco) => ({
      value: espaco.id,
      label: `${espaco.nome} (${this.tipoLabel(espaco.tipo)})${espaco.ativo ? '' : ' - INATIVO'}`,
      disabled: !espaco.ativo
    }));
  }

  get selectedEspaco(): Espaco | undefined {
    return this.filteredEspacos.find((espaco) => espaco.id === this.form.controls.espacoId.value);
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

  get canCreateReservation(): boolean {
    return !this.creating && this.form.valid && this.selectedSlots.length > 0;
  }

  get minDate(): string {
    return currentDateInputValue();
  }

  get maxDate(): string {
    return addDaysToCurrentDate(RESERVA_MAX_DAYS_AHEAD);
  }

  private observeFilters(): void {
    this.form.controls.tipo.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.clearSelectedSlots();

        const currentEspaco = this.form.controls.espacoId.value;
        this.ensureEspacoSelecionado();

        if (this.form.controls.espacoId.value === currentEspaco) {
          this.loadAgenda();
        }
      });

    this.form.controls.espacoId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.clearSelectedSlots();
        this.loadAgenda();
      });

    this.form.controls.data.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.clearSelectedSlots();
        this.loadAgenda();
      });
  }

  private loadEspacos(): void {
    this.loading = true;
    this.errorMessage = '';

    this.espacosService
      .list()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (espacos) => {
          this.espacos = [...espacos].sort((a, b) => a.nome.localeCompare(b.nome));
          this.applyInitialEspacoFromQuery();
          this.ensureEspacoSelecionado();
          this.loadAgenda();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar espaços.');
        }
      });
  }

  private ensureEspacoSelecionado(): void {
    const currentEspaco = this.form.controls.espacoId.value;
    const disponiveis = this.filteredEspacos.filter((espaco) => espaco.ativo);
    const fallbackEspacoId = (disponiveis[0] ?? this.filteredEspacos[0])?.id ?? 0;

    if (this.filteredEspacos.some((espaco) => espaco.id === currentEspaco)) {
      return;
    }

    this.form.controls.espacoId.setValue(fallbackEspacoId);
  }

  private applyInitialEspacoFromQuery(): void {
    const espacoIdParam = this.route.snapshot.queryParamMap.get('espacoId');
    const espacoId = Number(espacoIdParam);

    if (!Number.isInteger(espacoId) || espacoId <= 0) {
      return;
    }

    const espacoSelecionado = this.espacos.find((espaco) => espaco.id === espacoId && espaco.ativo);
    if (!espacoSelecionado) {
      return;
    }

    this.form.patchValue(
      {
        tipo: espacoSelecionado.tipo,
        espacoId: espacoSelecionado.id
      },
      { emitEvent: false }
    );
  }

  private loadAgenda(): void {
    const espacoId = this.form.controls.espacoId.value;
    const data = this.form.controls.data.value;

    this.agenda = null;
    this.agendaSlots = [];

    if (espacoId <= 0 || !data || this.form.controls.data.invalid) {
      return;
    }

    this.loadingAgenda = true;
    this.reservasService
      .agenda(espacoId, data)
      .pipe(finalize(() => (this.loadingAgenda = false)))
      .subscribe({
        next: (agenda) => {
          this.agenda = agenda;
          this.agendaSlots = buildAgendaSlots(agenda, {
            openingTime: this.selectedEspaco?.horarioFuncionamentoInicio,
            closingTime: this.selectedEspaco?.horarioFuncionamentoFim,
            fallbackOpeningMinutes: DEFAULT_OPENING_MINUTES,
            fallbackClosingMinutes: DEFAULT_CLOSING_MINUTES,
            slotDurationMinutes: SLOT_DURATION_MINUTES,
            isPastSlot: (slotStartMinutes) => this.isPastSlot(slotStartMinutes)
          });
        },
        error: (error: unknown) => {
          this.agenda = null;
          this.agendaSlots = [];
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar a agenda do dia.');
        }
      });
  }

  private clearSelectedSlots(): void {
    this.selectedSlotIndexes = [];
  }

  private isPastSlot(startMinutes: number): boolean {
    const selectedDate = this.form.controls.data.value;
    if (!selectedDate || selectedDate !== currentDateInputValue()) {
      return false;
    }

    return startMinutes <= this.currentMinutes();
  }

  private currentMinutes(): number {
    const now = new Date();
    return now.getHours() * 60 + now.getMinutes();
  }
}
