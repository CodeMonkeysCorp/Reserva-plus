import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Espaco, EspacoPayload, EspacoTipo } from '../../core/models';
import {
  collectEspacoTipos,
  CREATE_NEW_ESPACO_TIPO,
  DEFAULT_ESPACO_TIPOS,
  formatEspacoTipoLabel,
  normalizeEspacoTipo
} from '../../core/espaco-tipo';
import { ApiErrorService } from '../../core/services/api-error.service';
import { EspacosService } from '../../core/services/espacos.service';
import { SelectFieldComponent, SelectFieldOption } from '../../shared/ui/select-field/select-field.component';

type TipoSelectValue = EspacoTipo | typeof CREATE_NEW_ESPACO_TIPO;

@Component({
  selector: 'app-espacos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SelectFieldComponent],
  templateUrl: './espacos.component.html',
  styleUrl: './espacos.component.css'
})
export class EspacosComponent implements OnInit {
  @ViewChild('formPanel') private readonly formPanel?: ElementRef<HTMLElement>;

  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly espacosService = inject(EspacosService);
  private readonly apiErrorService = inject(ApiErrorService);

  readonly defaultTipo = DEFAULT_ESPACO_TIPOS[0];
  readonly createNewTypeValue = CREATE_NEW_ESPACO_TIPO;
  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    tipo: [this.defaultTipo as TipoSelectValue, [Validators.required]],
    novoTipo: ['', [Validators.maxLength(60)]],
    descricao: ['', [Validators.maxLength(500)]],
    horarioFuncionamentoInicio: ['06:00', [Validators.required]],
    horarioFuncionamentoFim: ['23:00', [Validators.required]]
  });

  espacos: Espaco[] = [];
  loading = true;
  saving = false;

  editingId: number | null = null;
  pendingDelete: Espaco | null = null;
  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.observeTipoSelection();
    this.loadEspacos();
  }

  submit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const horarioInicio = this.normalizeTime(this.form.controls.horarioFuncionamentoInicio.value);
    const horarioFim = this.normalizeTime(this.form.controls.horarioFuncionamentoFim.value);

    if (!this.isHoraCheia(horarioInicio) || !this.isHoraCheia(horarioFim)) {
      this.errorMessage = 'Defina o funcionamento apenas em horas cheias.';
      return;
    }

    if (horarioFim <= horarioInicio) {
      this.errorMessage = 'O horário final de funcionamento precisa ser maior que o inicial.';
      return;
    }

    const tipo = this.resolveTipoForSave();
    if (!tipo) {
      this.errorMessage = 'Informe o nome do novo tipo.';
      this.form.controls.novoTipo.markAsTouched();
      return;
    }

    const payload: EspacoPayload = {
      nome: this.form.controls.nome.value.trim(),
      tipo,
      descricao: this.form.controls.descricao.value.trim() || undefined,
      ativo: true,
      horarioFuncionamentoInicio: horarioInicio,
      horarioFuncionamentoFim: horarioFim
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
          this.successMessage = this.editingId === null ? 'Espaço criado com sucesso.' : 'Espaço atualizado com sucesso.';
          this.cancelEdit();
          this.loadEspacos();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao salvar espaço.');
        }
      });
  }

  edit(espaco: Espaco): void {
    this.editingId = espaco.id;
    this.pendingDelete = null;
    this.successMessage = '';
    this.errorMessage = '';
    this.form.patchValue({
      nome: espaco.nome,
      tipo: espaco.tipo,
      novoTipo: '',
      descricao: espaco.descricao ?? '',
      horarioFuncionamentoInicio: this.normalizeTime(espaco.horarioFuncionamentoInicio),
      horarioFuncionamentoFim: this.normalizeTime(espaco.horarioFuncionamentoFim)
    });
    this.scrollFormIntoView();
  }

  cancelEdit(): void {
    this.editingId = null;
    this.form.reset({
      nome: '',
      tipo: this.defaultTipo,
      novoTipo: '',
      descricao: '',
      horarioFuncionamentoInicio: '06:00',
      horarioFuncionamentoFim: '23:00'
    });
  }

  requestRemove(espaco: Espaco): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.pendingDelete = espaco;
  }

  cancelRemove(): void {
    if (this.saving) {
      return;
    }

    this.pendingDelete = null;
  }

  confirmRemove(): void {
    const espaco = this.pendingDelete;
    if (!espaco) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';
    this.saving = true;
    this.espacosService
      .delete(espaco.id)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.successMessage = 'Espaço removido com sucesso.';
          this.pendingDelete = null;
          if (this.editingId === espaco.id) {
            this.cancelEdit();
          }
          this.loadEspacos();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao remover espaço.');
        }
      });
  }

  get descricaoLength(): number {
    return this.form.controls.descricao.value.length;
  }

  get novoTipoLength(): number {
    return this.form.controls.novoTipo.value.length;
  }

  get isCreatingNewType(): boolean {
    return this.form.controls.tipo.value === CREATE_NEW_ESPACO_TIPO;
  }

  get tipoOptions(): Array<SelectFieldOption<TipoSelectValue>> {
    const selectedTipo = this.form.controls.tipo.value;
    const tipos = collectEspacoTipos(
      [
        ...this.espacos.map((espaco) => espaco.tipo),
        selectedTipo === CREATE_NEW_ESPACO_TIPO ? null : selectedTipo
      ],
      { includeDefaults: true }
    );

    return [
      ...tipos.map((tipo) => ({
        value: tipo,
        label: this.tipoLabel(tipo)
      })),
      {
        value: CREATE_NEW_ESPACO_TIPO,
        label: 'Criar tipo de espaço',
        variant: 'action'
      }
    ];
  }

  tipoLabel(tipo: EspacoTipo): string {
    return formatEspacoTipoLabel(tipo);
  }

  formatTime(value: string | null | undefined): string {
    return this.normalizeTime(value);
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
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar espaços.');
        }
      });
  }

  private observeTipoSelection(): void {
    this.form.controls.tipo.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((tipo) => {
      if (tipo !== CREATE_NEW_ESPACO_TIPO && this.form.controls.novoTipo.value) {
        this.form.controls.novoTipo.setValue('', { emitEvent: false });
      }
    });
  }

  private resolveTipoForSave(): EspacoTipo | null {
    if (!this.isCreatingNewType) {
      return normalizeEspacoTipo(this.form.controls.tipo.value);
    }

    const novoTipo = normalizeEspacoTipo(this.form.controls.novoTipo.value);
    return novoTipo || null;
  }

  private normalizeTime(value: string | null | undefined): string {
    return (value ?? '').slice(0, 5);
  }

  private isHoraCheia(value: string): boolean {
    return value.endsWith(':00');
  }

  private scrollFormIntoView(): void {
    this.formPanel?.nativeElement.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    });
  }
}
