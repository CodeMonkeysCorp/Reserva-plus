import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Espaco, EspacoPayload, EspacoTipo } from '../../core/models';
import { ApiErrorService } from '../../core/services/api-error.service';
import { EspacosService } from '../../core/services/espacos.service';

@Component({
  selector: 'app-espacos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './espacos.component.html',
  styleUrl: './espacos.component.css'
})
export class EspacosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly espacosService = inject(EspacosService);
  private readonly apiErrorService = inject(ApiErrorService);

  readonly tipos: EspacoTipo[] = ['QUADRA', 'QUIOSQUE'];
  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    tipo: ['QUADRA' as EspacoTipo, [Validators.required]],
    descricao: ['', [Validators.maxLength(500)]],
    ativo: [true]
  });

  espacos: Espaco[] = [];
  loading = true;
  saving = false;

  editingId: number | null = null;
  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.loadEspacos();
  }

  submit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: EspacoPayload = {
      nome: this.form.controls.nome.value.trim(),
      tipo: this.form.controls.tipo.value,
      descricao: this.form.controls.descricao.value.trim() || undefined,
      ativo: this.form.controls.ativo.value
    };

    this.saving = true;
    const request$ =
      this.editingId === null
        ? this.espacosService.create(payload)
        : this.espacosService.update(this.editingId, payload);

    request$
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.successMessage = this.editingId === null ? 'Espaco criado com sucesso.' : 'Espaco atualizado com sucesso.';
          this.cancelEdit();
          this.loadEspacos();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao salvar espaco.');
        }
      });
  }

  edit(espaco: Espaco): void {
    this.editingId = espaco.id;
    this.successMessage = '';
    this.errorMessage = '';
    this.form.patchValue({
      nome: espaco.nome,
      tipo: espaco.tipo,
      descricao: espaco.descricao ?? '',
      ativo: espaco.ativo
    });
  }

  cancelEdit(): void {
    this.editingId = null;
    this.form.reset({
      nome: '',
      tipo: 'QUADRA',
      descricao: '',
      ativo: true
    });
  }

  remove(espaco: Espaco): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!confirm(`Deseja remover o espaco "${espaco.nome}"?`)) {
      return;
    }

    this.saving = true;
    this.espacosService
      .delete(espaco.id)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.successMessage = 'Espaco removido com sucesso.';
          this.loadEspacos();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao remover espaco.');
        }
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
          this.espacos = espacos.sort((a, b) => a.nome.localeCompare(b.nome));
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar espacos.');
        }
      });
  }
}
