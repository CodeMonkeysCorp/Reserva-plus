import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Bloqueio, BloqueioPayload, Espaco } from '../../core/models';
import { ApiErrorService } from '../../core/services/api-error.service';
import { BloqueiosService } from '../../core/services/bloqueios.service';
import { EspacosService } from '../../core/services/espacos.service';

@Component({
  selector: 'app-bloqueios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './bloqueios.component.html',
  styleUrl: './bloqueios.component.css'
})
export class BloqueiosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly bloqueiosService = inject(BloqueiosService);
  private readonly espacosService = inject(EspacosService);
  private readonly apiErrorService = inject(ApiErrorService);

  readonly form = this.fb.nonNullable.group({
    espacoId: [0, [Validators.required, Validators.min(1)]],
    data: [this.today(), [Validators.required]],
    horarioInicio: ['08:00', [Validators.required]],
    horarioFim: ['10:00', [Validators.required]],
    motivo: ['', [Validators.maxLength(300)]]
  });

  espacos: Espaco[] = [];
  bloqueios: Bloqueio[] = [];

  loadingEspacos = true;
  loadingBloqueios = false;
  saving = false;
  deleting = false;

  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.loadEspacos();
  }

  criarBloqueio(): void {
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

    const payload: BloqueioPayload = {
      espacoId: this.form.controls.espacoId.value,
      data: this.form.controls.data.value,
      horarioInicio: this.form.controls.horarioInicio.value,
      horarioFim: this.form.controls.horarioFim.value,
      motivo: this.form.controls.motivo.value.trim() || undefined
    };

    this.saving = true;
    this.bloqueiosService
      .create(payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.successMessage = 'Bloqueio criado com sucesso.';
          this.carregarBloqueios();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao criar bloqueio.');
        }
      });
  }

  carregarBloqueios(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.form.controls.espacoId.value <= 0) {
      this.bloqueios = [];
      return;
    }

    this.loadingBloqueios = true;
    this.bloqueiosService
      .list(this.form.controls.espacoId.value, this.form.controls.data.value)
      .pipe(finalize(() => (this.loadingBloqueios = false)))
      .subscribe({
        next: (bloqueios) => {
          this.bloqueios = bloqueios;
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar bloqueios.');
        }
      });
  }

  removerBloqueio(bloqueio: Bloqueio): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!confirm(`Remover bloqueio de ${bloqueio.horarioInicio} - ${bloqueio.horarioFim}?`)) {
      return;
    }

    this.deleting = true;
    this.bloqueiosService
      .delete(bloqueio.id)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.successMessage = 'Bloqueio removido com sucesso.';
          this.carregarBloqueios();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao remover bloqueio.');
        }
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
          }
          this.carregarBloqueios();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar espacos.');
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
