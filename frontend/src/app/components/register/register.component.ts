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

  constructor() {
    if (this.authService.isAuthenticated()) {
      void this.router.navigate(['/home']);
    }
  }

  submit(): void {
    this.errorMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.form.controls.senha.value !== this.form.controls.confirmarSenha.value) {
      this.errorMessage = 'As senhas nao conferem.';
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
          this.errorMessage = this.apiErrorService.toMessage(error, 'Nao foi possivel concluir o cadastro.');
        }
      });
  }
}
