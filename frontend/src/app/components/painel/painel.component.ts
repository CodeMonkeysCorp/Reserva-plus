import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { finalize } from 'rxjs';
import {
  PainelAgendaItem,
  PainelRankingEspaco,
  PainelResumo,
  PainelTipoResumo
} from '../../core/models';
import { formatEspacoTipoLabel } from '../../core/espaco-tipo';
import { ApiErrorService } from '../../core/services/api-error.service';
import { PainelService } from '../../core/services/painel.service';

const AGENDA_PAGE_SIZE = 5;

interface CatalogoInsight {
  label: string;
  value: number;
  helper: string;
  tone: 'neutral' | 'positive' | 'warning';
}

interface UsuarioResumo {
  label: string;
  value: number;
  helper: string;
}

interface PainelStat {
  label: string;
  value: number;
}

interface PainelResumoChip {
  label: string;
  value: number;
  tone?: 'neutral' | 'live';
}

@Component({
  selector: 'app-painel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './painel.component.html',
  styleUrl: './painel.component.css'
})
export class PainelComponent implements OnInit {
  private readonly painelService = inject(PainelService);
  private readonly apiErrorService = inject(ApiErrorService);

  loading = true;
  agendaLoading = false;
  errorMessage = '';

  espacosAtivos = 0;
  espacosInativos = 0;
  espacosDestacados = 0;
  espacosSemImagem = 0;
  espacosSemDescricao = 0;
  reservasAtivas = 0;
  reservasCanceladas = 0;
  reservasConcluidas = 0;
  reservasHoje = 0;
  reservasEmAndamento = 0;
  reservasFuturas = 0;
  totalUsuarios = 0;
  totalAdmins = 0;
  usuariosComReserva = 0;
  usuariosSemReserva = 0;

  agendaReservas: PainelAgendaItem[] = [];
  agendaCurrentPage = 1;
  agendaTotalPages = 0;
  agendaTotalElements = 0;
  tiposResumo: PainelTipoResumo[] = [];
  topEspacos: PainelRankingEspaco[] = [];
  espacosConcorridos: PainelRankingEspaco[] = [];

  ngOnInit(): void {
    this.loadDashboard();
  }

  get agendaShowingStart(): number {
    return this.agendaTotalElements > 0 ? (this.agendaCurrentPage - 1) * AGENDA_PAGE_SIZE + 1 : 0;
  }

  get agendaShowingEnd(): number {
    return this.agendaTotalElements > 0
      ? Math.min(this.agendaCurrentPage * AGENDA_PAGE_SIZE, this.agendaTotalElements)
      : 0;
  }

  get dashboardStats(): PainelStat[] {
    return [
      { label: 'Espaços Ativos', value: this.espacosAtivos },
      { label: 'Espaços Inativos', value: this.espacosInativos },
      { label: 'Espaços em Destaque', value: this.espacosDestacados },
      { label: 'Reservas Ativas', value: this.reservasAtivas },
      { label: 'Reservas Futuras', value: this.reservasFuturas },
      { label: 'Reservas de Hoje', value: this.reservasHoje },
      { label: 'Usuários', value: this.totalUsuarios },
      { label: 'Usuários com Reserva', value: this.usuariosComReserva }
    ];
  }

  get agendaResumoChips(): PainelResumoChip[] {
    return [
      { label: 'Em andamento', value: this.reservasEmAndamento, tone: 'live' },
      { label: 'Futuras', value: this.reservasFuturas },
      { label: 'Canceladas', value: this.reservasCanceladas },
      { label: 'Concluídas', value: this.reservasConcluidas }
    ];
  }

  get catalogoInsights(): CatalogoInsight[] {
    return [
      {
        label: 'Em destaque',
        value: this.espacosDestacados,
        helper: this.espacosDestacados > 0
          ? 'Aparecem em destaque no Início.'
          : 'Nenhum espaço foi marcado para vitrine manual.',
        tone: this.espacosDestacados > 0 ? 'positive' : 'neutral'
      },
      {
        label: 'Sem imagem',
        value: this.espacosSemImagem,
        helper: this.espacosSemImagem > 0
          ? 'Vale completar o catálogo visual para melhorar a navegação.'
          : 'Todos os espaços têm imagem cadastrada.',
        tone: this.espacosSemImagem > 0 ? 'warning' : 'positive'
      },
      {
        label: 'Sem descrição',
        value: this.espacosSemDescricao,
        helper: this.espacosSemDescricao > 0
          ? 'Descrições ajudam o usuário a escolher melhor.'
          : 'Todos os espaços têm descrição preenchida.',
        tone: this.espacosSemDescricao > 0 ? 'warning' : 'positive'
      },
      {
        label: 'Inativos',
        value: this.espacosInativos,
        helper: this.espacosInativos > 0
          ? 'Esses espaços não entram no fluxo normal de reserva.'
          : 'Todos os espaços estão ativos para reserva.',
        tone: this.espacosInativos > 0 ? 'warning' : 'positive'
      }
    ];
  }

  get usuariosResumo(): UsuarioResumo[] {
    const usuariosComuns = this.totalUsuarios - this.totalAdmins;

    return [
      {
        label: 'Administradores',
        value: this.totalAdmins,
        helper: 'Perfis com acesso total às áreas administrativas.'
      },
      {
        label: 'Usuários comuns',
        value: usuariosComuns,
        helper: 'Público principal que consulta agenda e cria reservas.'
      },
      {
        label: 'Sem histórico de reserva',
        value: this.usuariosSemReserva,
        helper: 'Contas que ainda não passaram pelo fluxo de reserva.'
      }
    ];
  }

  formatTipo(tipo: string): string {
    return formatEspacoTipoLabel(tipo);
  }

  trackByAgendaReservaId(_index: number, item: PainelAgendaItem): number {
    return item.reserva.id;
  }

  trackByTipo(_index: number, item: PainelTipoResumo): string {
    return item.tipo;
  }

  trackByRankingEspaco(_index: number, item: PainelRankingEspaco): number {
    return item.espacoId;
  }

  trackByLabel(_index: number, item: { label: string }): string {
    return item.label;
  }

  previousAgendaPage(): void {
    if (this.agendaCurrentPage <= 1 || this.agendaLoading) {
      return;
    }

    this.loadDashboard(this.agendaCurrentPage - 2, true);
  }

  nextAgendaPage(): void {
    if (this.agendaCurrentPage >= this.agendaTotalPages || this.agendaLoading) {
      return;
    }

    this.loadDashboard(this.agendaCurrentPage, true);
  }

  private loadDashboard(page = 0, agendaOnly = false): void {
    if (agendaOnly) {
      this.agendaLoading = true;
    } else {
      this.loading = true;
      this.errorMessage = '';
    }

    this.painelService
      .resumo(page, AGENDA_PAGE_SIZE)
      .pipe(finalize(() => {
        this.loading = false;
        this.agendaLoading = false;
      }))
      .subscribe({
        next: (response) => {
          this.applyDashboardData(response);
        },
        error: (error: unknown) => {
          this.errorMessage = this.apiErrorService.toMessage(error, 'Falha ao carregar o painel administrativo.');
        }
      });
  }

  private applyDashboardData(response: PainelResumo): void {
    this.espacosAtivos = response.espacos.espacosAtivos;
    this.espacosInativos = response.espacos.espacosInativos;
    this.espacosDestacados = response.espacos.espacosDestacados;
    this.espacosSemImagem = response.espacos.espacosSemImagem;
    this.espacosSemDescricao = response.espacos.espacosSemDescricao;

    this.reservasAtivas = response.reservas.reservasAtivas;
    this.reservasCanceladas = response.reservas.reservasCanceladas;
    this.reservasConcluidas = response.reservas.reservasConcluidas;
    this.reservasHoje = response.reservas.reservasHoje;
    this.reservasEmAndamento = response.reservas.reservasEmAndamento;
    this.reservasFuturas = response.reservas.reservasFuturas;

    this.totalUsuarios = response.usuarios.totalUsuarios;
    this.totalAdmins = response.usuarios.totalAdmins;
    this.usuariosComReserva = response.usuarios.usuariosComReserva;
    this.usuariosSemReserva = response.usuarios.usuariosSemReserva;

    this.agendaReservas = response.agenda.items;
    this.agendaCurrentPage = response.agenda.page + 1;
    this.agendaTotalPages = response.agenda.totalPages;
    this.agendaTotalElements = response.agenda.totalElements;

    this.tiposResumo = response.tiposResumo;
    this.topEspacos = response.topEspacos;
    this.espacosConcorridos = response.espacosConcorridos;
  }
}
