import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
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
import { TimeFieldComponent } from '../../shared/ui/time-field/time-field.component';

type TipoSelectValue = EspacoTipo | typeof CREATE_NEW_ESPACO_TIPO;

interface PendingTipoRemoval {
  tipo: EspacoTipo;
  affectedEspacos: Espaco[];
}

@Component({
  selector: 'app-espacos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SelectFieldComponent, TimeFieldComponent],
  templateUrl: './espacos.component.html',
  styleUrl: './espacos.component.css'
})
export class EspacosComponent implements OnInit {
  @ViewChild('formPanel') private readonly formPanel?: ElementRef<HTMLElement>;
  @ViewChild('imageInput') private readonly imageInput?: ElementRef<HTMLInputElement>;

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
  readonly tipoReplacementControl = this.fb.nonNullable.control(this.defaultTipo, Validators.required);

  espacos: Espaco[] = [];
  loading = true;
  saving = false;
  imageUploading = false;

  editingId: number | null = null;
  pendingDelete: Espaco | null = null;
  pendingTipoRemoval: PendingTipoRemoval | null = null;
  successMessage = '';
  errorMessage = '';
  imageErrorMessage = '';
  selectedImageObjectKey: string | null = null;
  selectedImageUrl: string | null = null;
  selectedImageFileName = '';
  originalImageObjectKey: string | null = null;
  originalImageUrl: string | null = null;

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
      imagemObjectKey: this.selectedImageObjectKey,
      ativo: this.editingEspaco?.ativo ?? true,
      destaque: this.editingEspaco?.destaque ?? false,
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
          this.resetFormState();
          this.loadEspacos();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao salvar espaço.');
        }
      });
  }

  edit(espaco: Espaco): void {
    this.deleteDraftImageIfNeeded();
    this.editingId = espaco.id;
    this.pendingDelete = null;
    this.pendingTipoRemoval = null;
    this.successMessage = '';
    this.errorMessage = '';
    this.imageErrorMessage = '';
    this.form.patchValue({
      nome: espaco.nome,
      tipo: espaco.tipo,
      novoTipo: '',
      descricao: espaco.descricao ?? '',
      horarioFuncionamentoInicio: this.normalizeTime(espaco.horarioFuncionamentoInicio),
      horarioFuncionamentoFim: this.normalizeTime(espaco.horarioFuncionamentoFim)
    });
    this.originalImageObjectKey = espaco.imagemObjectKey ?? null;
    this.originalImageUrl = espaco.imagemUrl ?? null;
    this.selectedImageObjectKey = espaco.imagemObjectKey ?? null;
    this.selectedImageUrl = espaco.imagemUrl ?? null;
    this.selectedImageFileName = espaco.imagemObjectKey ? 'Imagem atual vinculada' : '';
    this.scrollFormIntoView();
  }

  cancelEdit(): void {
    this.deleteDraftImageIfNeeded();
    this.resetFormState();
  }

  requestRemove(espaco: Espaco): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.pendingTipoRemoval = null;
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
            this.resetFormState();
          }
          this.loadEspacos();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao remover espaço.');
        }
      });
  }

  requestRemoveTipoOption(option: SelectFieldOption<TipoSelectValue>): void {
    const tipo = this.normalizeTipoValue(option.value);
    if (!tipo || this.isDefaultTipo(tipo)) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';
    this.pendingDelete = null;
    this.pendingTipoRemoval = {
      tipo,
      affectedEspacos: this.findEspacosByTipo(tipo)
    };

    const replacementTipo = this.tipoReplacementOptions[0]?.value ?? this.defaultTipo;
    this.tipoReplacementControl.setValue(replacementTipo, { emitEvent: false });
    this.scrollFormIntoView();
  }

  cancelTipoRemoval(): void {
    if (this.saving) {
      return;
    }

    this.pendingTipoRemoval = null;
    this.tipoReplacementControl.setValue(this.defaultTipo, { emitEvent: false });
  }

  confirmTipoRemoval(): void {
    const pending = this.pendingTipoRemoval;
    if (!pending) {
      return;
    }

    const replacementTipo = this.normalizeTipoValue(this.tipoReplacementControl.value);
    if (!replacementTipo || replacementTipo === pending.tipo) {
      this.errorMessage = 'Escolha outro tipo para substituir o tipo removido.';
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    if (pending.affectedEspacos.length === 0) {
      this.finishTipoRemoval(pending.tipo, replacementTipo, 0);
      return;
    }

    this.saving = true;
    forkJoin(
      pending.affectedEspacos.map((espaco) =>
        this.espacosService.update(espaco.id, this.buildPayloadFromEspaco(espaco, replacementTipo))
      )
    )
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.finishTipoRemoval(pending.tipo, replacementTipo, pending.affectedEspacos.length);
          this.loadEspacos();
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao remover o tipo personalizado.');
        }
      });
  }

  toggleStatus(espaco: Espaco): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.pendingDelete = null;
    this.pendingTipoRemoval = null;

    const nextStatus = !espaco.ativo;

    this.saving = true;
    this.espacosService
      .update(espaco.id, this.buildPayloadFromEspaco(espaco, espaco.tipo, nextStatus))
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (updated) => {
          this.espacos = this.sortEspacos(this.espacos.map((item) => (item.id === updated.id ? updated : item)));

          if (this.editingId === updated.id) {
            this.syncEditingImageState(updated);
          }

          this.successMessage = updated.ativo
            ? `Espaço "${updated.nome}" ativado com sucesso.`
            : `Espaço "${updated.nome}" desativado com sucesso.`;
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao atualizar o status do espaço.');
        }
      });
  }

  toggleDestaque(espaco: Espaco): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.pendingDelete = null;
    this.pendingTipoRemoval = null;

    const nextDestaque = !espaco.destaque;

    this.saving = true;
    this.espacosService
      .update(espaco.id, this.buildPayloadFromEspaco(espaco, espaco.tipo, espaco.ativo, nextDestaque))
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (updated) => {
          this.espacos = this.sortEspacos(this.espacos.map((item) => (item.id === updated.id ? updated : item)));

          if (this.editingId === updated.id) {
            this.syncEditingImageState(updated);
          }

          this.successMessage = updated.destaque
            ? `Espaço "${updated.nome}" marcado como destaque.`
            : `Espaço "${updated.nome}" removido dos destaques.`;
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao atualizar o destaque do espaço.');
        }
      });
  }

  openImagePicker(): void {
    this.imageInput?.nativeElement.click();
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0];
    if (!file) {
      return;
    }

    const previousDraftKey =
      this.selectedImageObjectKey && this.selectedImageObjectKey !== this.originalImageObjectKey
        ? this.selectedImageObjectKey
        : null;

    this.imageUploading = true;
    this.imageErrorMessage = '';
    this.selectedImageFileName = file.name;
    this.espacosService
      .uploadImage(file)
      .pipe(
        finalize(() => {
          this.imageUploading = false;
          if (input) {
            input.value = '';
          }
        })
      )
      .subscribe({
        next: (response) => {
          this.selectedImageObjectKey = response.chaveObjeto;
          this.selectedImageUrl = response.urlPublica ?? null;
          this.deleteDraftImage(previousDraftKey, response.chaveObjeto);
        },
        error: (error: unknown) => {
          this.imageErrorMessage = this.apiErrorService.toMessage(error, 'Falha ao enviar a imagem.');
          this.selectedImageFileName = this.originalImageObjectKey ? 'Imagem atual vinculada' : '';
        }
      });
  }

  removeSelectedImage(): void {
    this.imageErrorMessage = '';

    if (!this.selectedImageObjectKey) {
      return;
    }

    if (this.selectedImageObjectKey !== this.originalImageObjectKey) {
      const draftKey = this.selectedImageObjectKey;
      this.imageUploading = true;
      this.espacosService
        .deleteImage(draftKey)
        .pipe(finalize(() => (this.imageUploading = false)))
        .subscribe({
          next: () => {
            this.selectedImageObjectKey = this.originalImageObjectKey;
            this.selectedImageUrl = this.originalImageUrl;
            this.selectedImageFileName = this.originalImageObjectKey ? 'Imagem atual vinculada' : '';
          },
          error: (error: unknown) => {
            this.imageErrorMessage = this.apiErrorService.toMessage(error, 'Falha ao remover a imagem enviada.');
          }
        });
      return;
    }

    this.selectedImageObjectKey = null;
    this.selectedImageUrl = null;
    this.selectedImageFileName = '';
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

  get hasSelectedImage(): boolean {
    return !!this.selectedImageObjectKey;
  }

  get editingEspaco(): Espaco | null {
    if (this.editingId === null) {
      return null;
    }

    return this.espacos.find((espaco) => espaco.id === this.editingId) ?? null;
  }

  get imagePickerLabel(): string {
    if (this.imageUploading) {
      return 'Enviando imagem...';
    }
    return this.selectedImageFileName || 'Nenhum arquivo selecionado';
  }

  get horarioFuncionamentoInicioMin(): string {
    return '00:00';
  }

  get horarioFuncionamentoInicioMax(): string {
    const horarioFim = this.normalizeTime(this.form.controls.horarioFuncionamentoFim.value);
    if (!horarioFim) {
      return '22:00';
    }

    return this.offsetHour(horarioFim, -1);
  }

  get horarioFuncionamentoFimMin(): string {
    const horarioInicio = this.normalizeTime(this.form.controls.horarioFuncionamentoInicio.value);
    if (!horarioInicio) {
      return '01:00';
    }

    return this.offsetHour(horarioInicio, 1);
  }

  get horarioFuncionamentoFimMax(): string {
    return '23:00';
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
        label: this.tipoLabel(tipo),
        hint: this.isDefaultTipo(tipo) ? undefined : this.tipoUsageHint(tipo),
        removable: !this.isDefaultTipo(tipo),
        removeAriaLabel: `Excluir o tipo ${this.tipoLabel(tipo)}`
      })),
      {
        value: CREATE_NEW_ESPACO_TIPO,
        label: 'Criar tipo de espaço',
        variant: 'action'
      }
    ];
  }

  get tipoReplacementOptions(): Array<SelectFieldOption<EspacoTipo>> {
    const tipoAtual = this.pendingTipoRemoval?.tipo ?? null;
    if (!tipoAtual) {
      return [];
    }

    return collectEspacoTipos(
      this.espacos
        .map((espaco) => espaco.tipo)
        .filter((tipo) => this.normalizeTipoValue(tipo) !== tipoAtual),
      { includeDefaults: true }
    ).map((tipo) => ({
      value: tipo,
      label: this.tipoLabel(tipo)
    }));
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
          this.espacos = this.sortEspacos(espacos);
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

  private finishTipoRemoval(tipoRemovido: EspacoTipo, replacementTipo: EspacoTipo, affectedCount: number): void {
    const tipoSelecionado = this.form.controls.tipo.value;
    if (tipoSelecionado !== CREATE_NEW_ESPACO_TIPO && this.normalizeTipoValue(tipoSelecionado) === tipoRemovido) {
      this.form.controls.tipo.setValue(replacementTipo);
    }

    this.pendingTipoRemoval = null;
    this.tipoReplacementControl.setValue(this.defaultTipo, { emitEvent: false });

    if (affectedCount === 0) {
      this.successMessage = `O tipo "${this.tipoLabel(tipoRemovido)}" foi removido da seleção.`;
      return;
    }

    const affectedLabel = affectedCount === 1 ? '1 espaço' : `${affectedCount} espaços`;
    this.successMessage =
      `Tipo "${this.tipoLabel(tipoRemovido)}" removido com sucesso e substituído por ` +
      `"${this.tipoLabel(replacementTipo)}" em ${affectedLabel}.`;
  }

  private buildPayloadFromEspaco(
    espaco: Espaco,
    tipo: EspacoTipo,
    ativo = espaco.ativo,
    destaque = espaco.destaque
  ): EspacoPayload {
    return {
      nome: espaco.nome.trim(),
      tipo,
      descricao: espaco.descricao?.trim() || undefined,
      imagemObjectKey: espaco.imagemObjectKey ?? null,
      ativo,
      destaque,
      horarioFuncionamentoInicio: this.normalizeTime(espaco.horarioFuncionamentoInicio),
      horarioFuncionamentoFim: this.normalizeTime(espaco.horarioFuncionamentoFim)
    };
  }

  private sortEspacos(espacos: Espaco[]): Espaco[] {
    return [...espacos].sort((left, right) => {
      const destaqueWeight = Number(right.destaque) - Number(left.destaque);
      if (destaqueWeight !== 0) {
        return destaqueWeight;
      }

      return left.nome.localeCompare(right.nome);
    });
  }

  private findEspacosByTipo(tipo: EspacoTipo): Espaco[] {
    return this.espacos.filter((espaco) => this.normalizeTipoValue(espaco.tipo) === tipo);
  }

  private countEspacosByTipo(tipo: EspacoTipo): number {
    return this.findEspacosByTipo(tipo).length;
  }

  private tipoUsageHint(tipo: EspacoTipo): string {
    const count = this.countEspacosByTipo(tipo);
    return count === 1 ? '1 espaço com esse tipo' : `${count} espaços com esse tipo`;
  }

  private isDefaultTipo(tipo: EspacoTipo): boolean {
    return DEFAULT_ESPACO_TIPOS.includes(tipo);
  }

  private normalizeTipoValue(value: TipoSelectValue | string | null | undefined): EspacoTipo {
    return normalizeEspacoTipo(value);
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

  private offsetHour(value: string, offset: number): string {
    const [hours] = value.split(':').map((part) => Number(part));
    const nextHour = Math.min(Math.max(hours + offset, 0), 23);
    return `${nextHour.toString().padStart(2, '0')}:00`;
  }

  private scrollFormIntoView(): void {
    this.formPanel?.nativeElement.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    });
  }

  private resetFormState(): void {
    this.editingId = null;
    this.pendingDelete = null;
    this.pendingTipoRemoval = null;
    this.form.reset({
      nome: '',
      tipo: this.defaultTipo,
      novoTipo: '',
      descricao: '',
      horarioFuncionamentoInicio: '06:00',
      horarioFuncionamentoFim: '23:00'
    });
    this.originalImageObjectKey = null;
    this.originalImageUrl = null;
    this.selectedImageObjectKey = null;
    this.selectedImageUrl = null;
    this.selectedImageFileName = '';
    this.imageErrorMessage = '';
    this.tipoReplacementControl.setValue(this.defaultTipo, { emitEvent: false });
  }

  private deleteDraftImageIfNeeded(): void {
    if (!this.selectedImageObjectKey || this.selectedImageObjectKey === this.originalImageObjectKey) {
      return;
    }

    this.deleteDraftImage(this.selectedImageObjectKey, null);
  }

  private deleteDraftImage(objectKey: string | null, nextObjectKey: string | null): void {
    if (!objectKey || objectKey === nextObjectKey) {
      return;
    }

    this.espacosService.deleteImage(objectKey).subscribe({
      error: () => {
        this.imageErrorMessage = 'Não foi possível limpar uma imagem temporária enviada anteriormente.';
      }
    });
  }

  private syncEditingImageState(espaco: Espaco): void {
    this.originalImageObjectKey = espaco.imagemObjectKey ?? null;
    this.originalImageUrl = espaco.imagemUrl ?? null;
    this.selectedImageObjectKey = espaco.imagemObjectKey ?? null;
    this.selectedImageUrl = espaco.imagemUrl ?? null;
    this.selectedImageFileName = espaco.imagemObjectKey ? 'Imagem atual vinculada' : '';
  }
}
