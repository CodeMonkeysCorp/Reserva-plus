import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { Espaco, Reserva } from '../../core/models';
import { collectEspacoTipos, formatEspacoTipoLabel } from '../../core/espaco-tipo';
import { ApiErrorService } from '../../core/services/api-error.service';
import { AuthService } from '../../core/services/auth.service';
import { EspacosService } from '../../core/services/espacos.service';
import { ReservasService } from '../../core/services/reservas.service';

const MAX_HOME_TIPOS = 6;
const MAX_FEATURED_ESPACOS = 3;

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

  featuredEspacos: Espaco[] = [];
  tiposDisponiveis: string[] = [];
  proximaReserva: Reserva | null = null;

  readonly isAdmin = this.authService.isAdmin();
  readonly nomeUsuario = this.authService.user?.nome ?? '';

  ngOnInit(): void {
    this.loadHome();
  }

  get heroDescription(): string {
    return this.isAdmin
      ? 'Explore os espaços cadastrados, acompanhe o que está disponível para reserva e acesse o painel administrativo quando precisar.'
      : 'Descubra ambientes disponíveis, veja sua próxima reserva e encontre rapidamente onde agendar seu próximo horário.';
  }

  formatTipo(tipo: string): string {
    return formatEspacoTipoLabel(tipo);
  }

  formatHorario(espaco: Espaco): string {
    return `${this.normalizeTime(espaco.horarioFuncionamentoInicio)} às ${this.normalizeTime(espaco.horarioFuncionamentoFim)}`;
  }

  trackByEspacoId(_index: number, espaco: Espaco): number {
    return espaco.id;
  }

  private loadHome(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      espacos: this.espacosService.list(),
      historico: this.reservasService.historico()
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ espacos, historico }) => {
          const ativos = espacos
            .filter((espaco) => espaco.ativo)
            .sort((a, b) => a.nome.localeCompare(b.nome));

          this.featuredEspacos = this.pickFeaturedEspacos(ativos);
          this.tiposDisponiveis = collectEspacoTipos(ativos.map((espaco) => espaco.tipo), { includeDefaults: false })
            .map((tipo) => this.formatTipo(tipo))
            .slice(0, MAX_HOME_TIPOS);
          this.proximaReserva = this.findNextReserva(historico);
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar a tela inicial.');
        }
      });
  }

  private pickFeaturedEspacos(espacos: Espaco[]): Espaco[] {
    const espacosDestacados = espacos.filter((espaco) => espaco.destaque);
    const candidatos = espacosDestacados.length > 0 ? espacosDestacados : espacos;

    return [...candidatos]
      .sort((left, right) => {
        const destaqueWeight = Number(right.destaque) - Number(left.destaque);
        if (destaqueWeight !== 0) {
          return destaqueWeight;
        }

        const imageWeight = Number(!!right.imagemUrl) - Number(!!left.imagemUrl);
        if (imageWeight !== 0) {
          return imageWeight;
        }

        const descriptionWeight = Number(!!right.descricao) - Number(!!left.descricao);
        if (descriptionWeight !== 0) {
          return descriptionWeight;
        }

        return left.nome.localeCompare(right.nome);
      })
      .slice(0, MAX_FEATURED_ESPACOS);
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
      .sort((left, right) => left.inicio.getTime() - right.inicio.getTime());

    return futuras.length > 0 ? futuras[0].reserva : null;
  }

  private normalizeTime(value: string | null | undefined): string {
    return (value ?? '').slice(0, 5);
  }
}
