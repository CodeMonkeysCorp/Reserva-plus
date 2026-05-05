import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { RegisterPayload } from '../../core/models';
import { ApiErrorService } from '../../core/services/api-error.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly apiErrorService = inject(ApiErrorService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
    confirmarSenha: ['', [Validators.required]]
  });

  loading = false;
  errorMessage = '';
  showSenha = false;
  showConfirmarSenha = false;

  constructor() {
    if (this.authService.isAuthenticated()) {
      void this.router.navigate(['/home']);
    }
  }

  submit(): void {
    this.errorMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage = 'Revise os campos destacados para continuar.';
      return;
    }

    if (this.form.controls.senha.value !== this.form.controls.confirmarSenha.value) {
      this.form.controls.confirmarSenha.markAsTouched();
      this.errorMessage = 'As senhas não conferem.';
      return;
    }

    const payload: RegisterPayload = {
      nome: this.form.controls.nome.value.trim(),
      email: this.form.controls.email.value.trim().toLowerCase(),
      senha: this.form.controls.senha.value
    };

    this.loading = true;
    this.authService
      .register(payload)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: () => {
          void this.router.navigate(['/home']);
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Não foi possível concluir o cadastro.');
        }
      });
  }

  toggleSenha(): void {
    this.showSenha = !this.showSenha;
  }

  toggleConfirmarSenha(): void {
    this.showConfirmarSenha = !this.showConfirmarSenha;
  }

  get senhasNaoConferem(): boolean {
    return (
      this.form.controls.confirmarSenha.touched &&
      this.form.controls.senha.value.length > 0 &&
      this.form.controls.confirmarSenha.value.length > 0 &&
      this.form.controls.senha.value !== this.form.controls.confirmarSenha.value
    );
  }
}
