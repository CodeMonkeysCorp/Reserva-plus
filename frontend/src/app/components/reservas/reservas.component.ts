import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { Espaco, Reserva, ReservaCreatePayload } from '../../core/models';
import { ApiErrorService } from '../../core/services/api-error.service';
import { AuthService } from '../../core/services/auth.service';
import { EspacosService } from '../../core/services/espacos.service';
import { ReservasService } from '../../core/services/reservas.service';

@Component({
  selector: 'app-reservas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reservas.component.html',
  styleUrl: './reservas.component.css'
})
export class ReservasComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly reservasService = inject(ReservasService);
  private readonly espacosService = inject(EspacosService);
  private readonly authService = inject(AuthService);
  private readonly apiErrorService = inject(ApiErrorService);

  readonly form = this.fb.nonNullable.group({
    espacoId: [0, [Validators.required, Validators.min(1)]],
    data: [this.today(), [Validators.required]],
    horarioInicio: ['18:00', [Validators.required]],
    horarioFim: ['19:00', [Validators.required]]
  });

  espacos: Espaco[] = [];
  historico: Reserva[] = [];

  loading = true;
  creating = false;
  cancelling = false;

  successMessage = '';
  errorMessage = '';

  readonly isAdmin = this.authService.isAdmin();

  ngOnInit(): void {
    this.loadInitialData();
  }

  criarReserva(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.isIntervaloValido(this.form.controls.horarioInicio.value, this.form.controls.horarioFim.value)) {
      this.errorMessage = 'Horario final deve ser maior que o horario inicial.';
      return;
    }

    const payload: ReservaCreatePayload = this.form.getRawValue();
    this.creating = true;

    this.reservasService
      .create(payload)
      .pipe(finalize(() => (this.creating = false)))
      .subscribe({
        next: () => {
          this.successMessage = 'Reserva criada com sucesso.';
          this.loadHistorico();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao criar reserva.');
        }
      });
  }

  cancelarReserva(reserva: Reserva): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!confirm(`Cancelar reserva de ${reserva.espacoNome} em ${reserva.data}?`)) {
      return;
    }

    this.cancelling = true;
    this.reservasService
      .cancel(reserva.id)
      .pipe(finalize(() => (this.cancelling = false)))
      .subscribe({
        next: () => {
          this.successMessage = 'Reserva cancelada.';
          this.loadHistorico();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao cancelar reserva.');
        }
      });
  }

  private loadInitialData(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      espacos: this.espacosService.list(),
      historico: this.reservasService.historico()
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ espacos, historico }) => {
          this.espacos = espacos.sort((a, b) => a.nome.localeCompare(b.nome));
          this.historico = historico;
          if (this.form.controls.espacoId.value === 0 && this.espacos.length > 0) {
            const defaultEspacoId = this.espacos.find((espaco) => espaco.ativo)?.id ?? this.espacos[0].id;
            this.form.patchValue({ espacoId: defaultEspacoId });
          }
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar dados de reserva.');
        }
      });
  }

  private loadHistorico(): void {
    this.reservasService.historico().subscribe({
      next: (historico) => {
        this.historico = historico;
      },
      error: (error: unknown) => {
        this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao atualizar historico.');
      }
    });
  }

  private today(): string {
    const now = new Date();
    const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
    return localDate.toISOString().split('T')[0];
  }

  private isIntervaloValido(inicio: string, fim: string): boolean {
    return this.timeToMinutes(fim) > this.timeToMinutes(inicio);
  }

  private timeToMinutes(value: string): number {
    const [hours, minutes] = value.split(':').map((part) => Number(part));
    return hours * 60 + minutes;
  }
}
