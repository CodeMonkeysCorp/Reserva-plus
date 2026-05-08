import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { collectEspacoTipos, formatEspacoTipoLabel } from '../../core/espaco-tipo';
import { Espaco, EspacoTipo, Reserva, ReservaStatus } from '../../core/models';
import { ApiErrorService } from '../../core/services/api-error.service';
import { EspacosService } from '../../core/services/espacos.service';
import { ReservasService } from '../../core/services/reservas.service';
import { DateFieldComponent } from '../../shared/ui/date-field/date-field.component';
import { SelectFieldComponent } from '../../shared/ui/select-field/select-field.component';

type FiltroTipoEspaco = 'TODOS' | EspacoTipo;
type FiltroStatusReserva = 'TODOS' | ReservaStatus;
type ViewMode = 'user' | 'admin';

@Component({
  selector: 'app-historico-reservas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, SelectFieldComponent, DateFieldComponent],
  templateUrl: './historico-reservas.component.html',
  styleUrl: './historico-reservas.component.css'
})
export class HistoricoReservasComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly reservasService = inject(ReservasService);
  private readonly espacosService = inject(EspacosService);
  private readonly apiErrorService = inject(ApiErrorService);

  private readonly viewMode: ViewMode = this.route.snapshot.data['view'] === 'admin' ? 'admin' : 'user';

  readonly statusFiltro: Array<{ value: FiltroStatusReserva; label: string }> = [
    { value: 'TODOS', label: 'Todos os status' },
    { value: 'ATIVA', label: 'Ativa' },
    { value: 'CONCLUIDA', label: 'Concluída' },
    { value: 'CANCELADA', label: 'Cancelada' }
  ];

  readonly form = this.fb.nonNullable.group({
    tipo: ['TODOS' as FiltroTipoEspaco],
    espacoId: [0],
    status: ['TODOS' as FiltroStatusReserva],
    data: ['']
  });

  espacos: Espaco[] = [];
  reservas: Reserva[] = [];
  pendingCancel: Reserva | null = null;

  loading = true;
  cancelling = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.observeFilters();
    this.loadData();
  }

  get isAdminView(): boolean {
    return this.viewMode === 'admin';
  }

  get pageTitle(): string {
    return this.isAdminView ? 'Gerenciar reservas' : 'Minhas reservas';
  }

  get pageSubtitle(): string {
    return this.isAdminView
      ? 'Acompanhe as reservas do sistema, filtre por espaço e intervenha quando necessário.'
      : 'Consulte seu histórico, filtre por tipo de espaço e cancele quando precisar.';
  }

  get emptyStateTitle(): string {
    return this.isAdminView ? 'Nenhuma reserva encontrada' : 'Você ainda não tem reservas';
  }

  get emptyStateDescription(): string {
    return this.hasActiveFilters
      ? 'Tente ajustar os filtros para encontrar outras reservas.'
      : this.isAdminView
        ? 'Assim que o sistema receber reservas, elas aparecerão aqui.'
        : 'Quando você reservar um espaço, ele aparecerá nesta tela.';
  }

  get filteredEspacos(): Espaco[] {
    const tipo = this.form.controls.tipo.value;
    if (tipo === 'TODOS') {
      return this.espacos;
    }

    return this.espacos.filter((espaco) => espaco.tipo === tipo);
  }

  get filteredReservas(): Reserva[] {
    const tipo = this.form.controls.tipo.value;
    const espacoId = this.form.controls.espacoId.value;
    const status = this.form.controls.status.value;
    const data = this.form.controls.data.value;

    return this.reservas
      .filter((reserva) => {
        const espaco = this.findEspaco(reserva.espacoId);
        const matchesTipo = tipo === 'TODOS' || espaco?.tipo === tipo;
        const matchesEspaco = espacoId === 0 || reserva.espacoId === espacoId;
        const matchesStatus = status === 'TODOS' || reserva.status === status;
        const matchesData = !data || reserva.data === data;

        return matchesTipo && matchesEspaco && matchesStatus && matchesData;
      })
      .sort((a, b) => {
        const dataA = `${a.data}T${a.horarioInicio}`;
        const dataB = `${b.data}T${b.horarioInicio}`;
        return dataB.localeCompare(dataA);
      });
  }

  get resultsLabel(): string {
    const total = this.filteredReservas.length;
    return total === 1 ? '1 resultado' : `${total} resultados`;
  }

  get hasReservations(): boolean {
    return this.filteredReservas.length > 0;
  }

  get hasActiveFilters(): boolean {
    return (
      this.form.controls.tipo.value !== 'TODOS' ||
      this.form.controls.espacoId.value !== 0 ||
      this.form.controls.status.value !== 'TODOS' ||
      !!this.form.controls.data.value
    );
  }

  get canCloseModal(): boolean {
    return !this.cancelling;
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

  get espacoOptions(): Array<{ value: number; label: string }> {
    return [
      { value: 0, label: 'Todos os espaços' },
      ...this.filteredEspacos.map((espaco) => ({
        value: espaco.id,
        label: espaco.nome
      }))
    ];
  }

  get statusOptions(): Array<{ value: FiltroStatusReserva; label: string }> {
    return this.statusFiltro;
  }

  openCancelModal(reserva: Reserva): void {
    if (reserva.status !== 'ATIVA') {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';
    this.pendingCancel = reserva;
  }

  closeCancelModal(): void {
    if (!this.canCloseModal) {
      return;
    }

    this.pendingCancel = null;
  }

  confirmCancel(): void {
    const reserva = this.pendingCancel;
    if (!reserva) {
      return;
    }

    this.cancelling = true;
    this.reservasService
      .cancel(reserva.id)
      .pipe(finalize(() => (this.cancelling = false)))
      .subscribe({
        next: (updated) => {
          this.reservas = this.reservas.map((item) => (item.id === updated.id ? updated : item));
          this.pendingCancel = null;
          this.successMessage = 'Reserva cancelada com sucesso.';
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao cancelar reserva.');
        }
      });
  }

  clearFilters(): void {
    this.form.setValue({
      tipo: 'TODOS',
      espacoId: 0,
      status: 'TODOS',
      data: ''
    });
  }

  tipoLabel(tipo: EspacoTipo | null | undefined): string {
    if (!tipo) {
      return 'Não informado';
    }

    return formatEspacoTipoLabel(tipo);
  }

  formatDateLabel(date: string): string {
    const [year, month, day] = date.split('-');
    return `${day}/${month}/${year}`;
  }

  reservaTipo(reserva: Reserva): EspacoTipo | null {
    return this.findEspaco(reserva.espacoId)?.tipo ?? null;
  }

  statusLabel(status: ReservaStatus): string {
    switch (status) {
      case 'ATIVA':
        return 'Ativa';
      case 'CONCLUIDA':
        return 'Concluída';
      default:
        return 'Cancelada';
    }
  }

  private observeFilters(): void {
    this.form.controls.tipo.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const espacoId = this.form.controls.espacoId.value;
        if (espacoId === 0) {
          return;
        }

        const stillAvailable = this.filteredEspacos.some((espaco) => espaco.id === espacoId);
        if (!stillAvailable) {
          this.form.controls.espacoId.setValue(0);
        }
      });
  }

  private loadData(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      espacos: this.espacosService.list(),
      reservas: this.reservasService.historico()
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ espacos, reservas }) => {
          this.espacos = [...espacos].sort((a, b) => a.nome.localeCompare(b.nome));
          this.reservas = reservas;
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar reservas.');
        }
      });
  }

  private findEspaco(espacoId: number): Espaco | undefined {
    return this.espacos.find((espaco) => espaco.id === espacoId);
  }
}
