import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AgendaDia, Espaco, EspacoTipo, ReservaCreatePayload } from '../../core/models';
import { ApiErrorService } from '../../core/services/api-error.service';
import { AuthService } from '../../core/services/auth.service';
import { EspacosService } from '../../core/services/espacos.service';
import { ReservasService } from '../../core/services/reservas.service';

type FiltroTipoEspaco = 'TODOS' | EspacoTipo;
type AgendaSlotStatus = 'LIVRE' | 'RESERVADO' | 'BLOQUEADO' | 'ENCERRADO';

interface AgendaSlot {
  index: number;
  inicio: string;
  fim: string;
  status: AgendaSlotStatus;
  detalhe: string;
}

const SLOT_DURATION_MINUTES = 60;
const DEFAULT_OPENING_MINUTES = 6 * 60;
const DEFAULT_CLOSING_MINUTES = 23 * 60;
const RESERVA_MAX_DAYS_AHEAD = 7;
const BOOKING_DATE_RANGE_ERROR = 'bookingDateRange';

function toDateInputValue(date: Date): string {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return localDate.toISOString().split('T')[0];
}

function currentDateInputValue(): string {
  return toDateInputValue(new Date());
}

function addDaysToCurrentDate(days: number): string {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() + days);
  return toDateInputValue(date);
}

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
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reservas.component.html',
  styleUrl: './reservas.component.css'
})
export class ReservasComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly reservasService = inject(ReservasService);
  private readonly espacosService = inject(EspacosService);
  private readonly authService = inject(AuthService);
  private readonly apiErrorService = inject(ApiErrorService);

  readonly tiposFiltro: Array<{ value: FiltroTipoEspaco; label: string }> = [
    { value: 'TODOS', label: 'Todos os tipos' },
    { value: 'QUADRA', label: 'Quadra' },
    { value: 'QUIOSQUE', label: 'Quiosque' }
  ];

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

    if (slot.status === 'ENCERRADO') {
      return 'Concluído';
    }

    return 'Reservado';
  }

  tipoLabel(tipo: EspacoTipo): string {
    return tipo === 'QUADRA' ? 'Quadra' : 'Quiosque';
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

    return `${this.normalizeTime(this.selectedEspaco.horarioFuncionamentoInicio)} as ${this.normalizeTime(this.selectedEspaco.horarioFuncionamentoFim)}`;
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
    return `${primeiraFaixa.inicio} as ${ultimaFaixa.fim}`;
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
          this.agendaSlots = this.buildAgendaSlots(agenda);
        },
        error: (error: unknown) => {
          this.agenda = null;
          this.agendaSlots = [];
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

      if (this.isPastSlot(start)) {
        slots.push({
          index,
          inicio,
          fim,
          status: 'ENCERRADO',
          detalhe: 'Horário concluído.'
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

  private minutesToTime(value: number): string {
    const hours = Math.floor(value / 60)
      .toString()
      .padStart(2, '0');
    const minutes = (value % 60).toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  }

  private timeToMinutes(value: string): number {
    const [hours, minutes] = value.split(':').map((part) => Number(part));
    return hours * 60 + minutes;
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
}
