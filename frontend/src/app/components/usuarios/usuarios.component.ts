import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AdminUser, AdminUserUpdatePayload, UserRole } from '../../core/models';
import { ApiErrorService } from '../../core/services/api-error.service';
import { AuthService } from '../../core/services/auth.service';
import { UsuariosService } from '../../core/services/usuarios.service';

type FiltroRole = 'TODOS' | UserRole;

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css'
})
export class UsuariosComponent implements OnInit {
  @ViewChild('editorPanel') private readonly editorPanel?: ElementRef<HTMLElement>;

  private readonly fb = inject(FormBuilder);
  private readonly usuariosService = inject(UsuariosService);
  private readonly authService = inject(AuthService);
  private readonly apiErrorService = inject(ApiErrorService);

  readonly roleOptions: Array<{ value: UserRole; label: string }> = [
    { value: 'ADMIN', label: 'Administrador' },
    { value: 'USER', label: 'Usuário' }
  ];

  readonly filtroForm = this.fb.nonNullable.group({
    busca: [''],
    role: ['TODOS' as FiltroRole]
  });

  readonly form = this.fb.nonNullable.group({
    role: ['USER' as UserRole, [Validators.required]],
    senha: ['', [Validators.minLength(6), Validators.maxLength(100)]],
    confirmacaoSenha: ['']
  });

  usuarios: AdminUser[] = [];
  loading = true;
  saving = false;
  editingId: number | null = null;

  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.loadUsuarios();
  }

  get currentUserId(): number | null {
    return this.authService.user?.id ?? null;
  }

  get selectedUser(): AdminUser | null {
    return this.usuarios.find((usuario) => usuario.id === this.editingId) ?? null;
  }

  get filteredUsuarios(): AdminUser[] {
    const busca = this.filtroForm.controls.busca.value.trim().toLowerCase();
    const role = this.filtroForm.controls.role.value;

    return this.usuarios.filter((usuario) => {
      const matchesRole = role === 'TODOS' || usuario.role === role;
      const matchesBusca =
        busca.length === 0 ||
        usuario.nome.toLowerCase().includes(busca) ||
        usuario.email.toLowerCase().includes(busca);

      return matchesRole && matchesBusca;
    });
  }

  get resultsLabel(): string {
    const total = this.filteredUsuarios.length;
    return total === 1 ? '1 usuário' : `${total} usuários`;
  }

  get passwordMismatch(): boolean {
    const senha = this.form.controls.senha.value;
    const confirmacao = this.form.controls.confirmacaoSenha.value;
    return senha.length > 0 && confirmacao.length > 0 && senha !== confirmacao;
  }

  get canEditRole(): boolean {
    return !!this.selectedUser && this.selectedUser.id !== this.currentUserId;
  }

  get canSubmit(): boolean {
    return !this.saving && this.selectedUser !== null;
  }

  edit(usuario: AdminUser): void {
    this.editingId = usuario.id;
    this.successMessage = '';
    this.errorMessage = '';
    this.form.reset({
      role: usuario.role,
      senha: '',
      confirmacaoSenha: ''
    });
    this.scrollEditorIntoView();
  }

  cancelEdit(): void {
    this.editingId = null;
    this.form.reset({
      role: 'USER',
      senha: '',
      confirmacaoSenha: ''
    });
  }

  submit(): void {
    const usuario = this.selectedUser;
    if (!usuario) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    if (this.form.invalid || this.passwordMismatch) {
      this.form.markAllAsTouched();
      this.errorMessage = this.passwordMismatch
        ? 'A confirmação da senha precisa ser igual à nova senha.'
        : 'Revise os campos informados antes de salvar.';
      return;
    }

    const senha = this.form.controls.senha.value.trim();
    const payload: AdminUserUpdatePayload = {
      role: this.canEditRole ? this.form.controls.role.value : usuario.role,
      senha: senha || undefined
    };

    this.saving = true;
    this.usuariosService
      .update(usuario.id, payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (updated) => {
          this.usuarios = this.usuarios
            .map((item) => (item.id === updated.id ? updated : item))
            .sort((a, b) => a.nome.localeCompare(b.nome) || a.email.localeCompare(b.email));

          this.editingId = updated.id;
          this.form.reset({
            role: updated.role,
            senha: '',
            confirmacaoSenha: ''
          });
          this.successMessage = 'Usuário atualizado com sucesso.';
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao atualizar usuário.');
        }
      });
  }

  roleLabel(role: UserRole): string {
    return role === 'ADMIN' ? 'Administrador' : 'Usuário';
  }

  private loadUsuarios(): void {
    this.loading = true;
    this.errorMessage = '';

    this.usuariosService
      .list()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (usuarios) => {
          this.usuarios = [...usuarios].sort((a, b) => a.nome.localeCompare(b.nome) || a.email.localeCompare(b.email));
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar usuários.');
        }
      });
  }

  private scrollEditorIntoView(): void {
    this.editorPanel?.nativeElement.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    });
  }
}
