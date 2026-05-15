export type UserRole = 'ADMIN' | 'USER';
export type EspacoTipo = string;
export type ReservaStatus = 'ATIVA' | 'CONCLUIDA' | 'CANCELADA';

export interface HorarioIntervalo {
  horarioInicio: string;
  horarioFim: string;
}

export interface DataHorarioIntervalo extends HorarioIntervalo {
  data: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EspacoResumo {
  espacoId: number;
  espacoNome: string;
}

export interface LoginPayload {
  email: string;
  senha: string;
}

export interface RegisterPayload extends LoginPayload {
  nome: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  nome: string;
  email: string;
  role: UserRole;
}

export interface AdminUser {
  id: number;
  nome: string;
  email: string;
  role: UserRole;
}

export interface AdminUserUpdatePayload {
  role: UserRole;
  senha?: string;
}

export interface EspacoEditableFields {
  nome: string;
  tipo: EspacoTipo;
  descricao?: string;
  imagemObjectKey?: string | null;
  ativo: boolean;
  destaque: boolean;
  horarioFuncionamentoInicio: string;
  horarioFuncionamentoFim: string;
}

export type EspacoPayload = EspacoEditableFields;

export interface Espaco extends EspacoEditableFields {
  id: number;
  imagemUrl?: string | null;
}

export interface EspacoImagemUploadResponse {
  chaveObjeto: string;
  urlPublica?: string | null;
  nomeArquivoOriginal: string;
  contentType: string;
  tamanhoBytes: number;
}

export interface Reserva extends DataHorarioIntervalo, EspacoResumo {
  id: number;
  usuarioId: number | null;
  usuarioNome: string | null;
  status: ReservaStatus;
  criadoEm: string;
}

export interface ReservaCreatePayload extends DataHorarioIntervalo {
  espacoId: number;
}

export interface PainelEspacoMetricas {
  totalEspacos: number;
  espacosAtivos: number;
  espacosInativos: number;
  espacosDestacados: number;
  espacosSemImagem: number;
  espacosSemDescricao: number;
}

export interface PainelReservaMetricas {
  reservasAtivas: number;
  reservasCanceladas: number;
  reservasConcluidas: number;
  reservasHoje: number;
  reservasEmAndamento: number;
  reservasFuturas: number;
}

export interface PainelUsuarioMetricas {
  totalUsuarios: number;
  totalAdmins: number;
  usuariosComReserva: number;
  usuariosSemReserva: number;
}

export interface PainelAgendaItem {
  reserva: Reserva;
  emAndamento: boolean;
}

export type PainelAgendaPage = PageResponse<PainelAgendaItem>;

export interface PainelTipoResumo {
  tipo: EspacoTipo;
  total: number;
  ativos: number;
  percentual: number;
}

export interface PainelRankingEspaco {
  espacoId: number;
  nome: string;
  tipo: EspacoTipo;
  totalReservas: number;
  futuras: number;
  agendaAtiva: number;
  destaque: boolean;
}

export interface PainelResumo {
  espacos: PainelEspacoMetricas;
  reservas: PainelReservaMetricas;
  usuarios: PainelUsuarioMetricas;
  agenda: PainelAgendaPage;
  tiposResumo: PainelTipoResumo[];
  topEspacos: PainelRankingEspaco[];
  espacosConcorridos: PainelRankingEspaco[];
}

export interface Bloqueio extends DataHorarioIntervalo, EspacoResumo {
  id: number;
  motivo?: string;
  serieRecorrenciaId?: string | null;
}

export interface BloqueioRotina extends HorarioIntervalo, EspacoResumo {
  id: number;
  serieRecorrenciaId: string;
  dataInicio: string;
  dataFim: string;
  motivo?: string;
  totalOcorrencias: number;
}

export interface AgendaDia {
  reservasAtivas: Reserva[];
  bloqueios: Bloqueio[];
}

export interface BloqueioPayload extends DataHorarioIntervalo {
  espacoId: number;
  motivo?: string;
  recorrenteSemanal?: boolean;
  dataFimRecorrencia?: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
