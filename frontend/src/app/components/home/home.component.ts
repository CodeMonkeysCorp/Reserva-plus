import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { Reserva } from '../../core/models';
import { ApiErrorService } from '../../core/services/api-error.service';
import { AuthService } from '../../core/services/auth.service';
import { EspacosService } from '../../core/services/espacos.service';
import { ReservasService } from '../../core/services/reservas.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly espacosService = inject(EspacosService);
  private readonly reservasService = inject(ReservasService);
  private readonly apiErrorService = inject(ApiErrorService);

  loading = true;
  errorMessage = '';

  totalEspacos = 0;
  reservasAtivas = 0;
  reservasCanceladas = 0;
  proximaReserva: Reserva | null = null;

  readonly isAdmin = this.authService.isAdmin();
  readonly nomeUsuario = this.authService.user?.nome ?? '';

  ngOnInit(): void {
    this.loadDashboard();
  }

  private loadDashboard(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      espacos: this.espacosService.list(),
      historico: this.reservasService.historico()
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ espacos, historico }) => {
          this.totalEspacos = espacos.length;
          this.reservasAtivas = historico.filter((item) => item.status === 'ATIVA').length;
          this.reservasCanceladas = historico.filter((item) => item.status === 'CANCELADA').length;
          this.proximaReserva = this.findNextReserva(historico);
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar o painel.');
        }
      });
  }

  private findNextReserva(reservas: Reserva[]): Reserva | null {
    const now = new Date();
    const futuras = reservas
      .filter((reserva) => reserva.status === 'ATIVA')
      .map((reserva) => ({
        reserva,
        inicio: new Date(`${reserva.data}T${reserva.horarioInicio}`)
      }))
      .filter((item) => item.inicio.getTime() >= now.getTime())
      .sort((a, b) => a.inicio.getTime() - b.inicio.getTime());

    return futuras.length > 0 ? futuras[0].reserva : null;
  }
}
