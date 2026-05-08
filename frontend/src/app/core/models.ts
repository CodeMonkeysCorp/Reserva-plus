export type UserRole = 'ADMIN' | 'USER';
export type EspacoTipo = string;
export type ReservaStatus = 'ATIVA' | 'CONCLUIDA' | 'CANCELADA';

export interface LoginPayload {
  email: string;
  senha: string;
}

export interface RegisterPayload {
  nome: string;
  email: string;
  senha: string;
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

export interface Espaco {
  id: number;
  nome: string;
  tipo: EspacoTipo;
  descricao?: string;
  ativo: boolean;
  horarioFuncionamentoInicio: string;
  horarioFuncionamentoFim: string;
}

export interface EspacoPayload {
  nome: string;
  tipo: EspacoTipo;
  descricao?: string;
  ativo: boolean;
  horarioFuncionamentoInicio: string;
  horarioFuncionamentoFim: string;
}

export interface Reserva {
  id: number;
  usuarioId: number | null;
  usuarioNome: string | null;
  espacoId: number;
  espacoNome: string;
  data: string;
  horarioInicio: string;
  horarioFim: string;
  status: ReservaStatus;
  criadoEm: string;
}

export interface ReservaCreatePayload {
  espacoId: number;
  data: string;
  horarioInicio: string;
  horarioFim: string;
}

export interface AgendaDia {
  reservasAtivas: Reserva[];
  bloqueios: Bloqueio[];
}

export interface Bloqueio {
  id: number;
  espacoId: number;
  espacoNome: string;
  data: string;
  horarioInicio: string;
  horarioFim: string;
  motivo?: string;
  serieRecorrenciaId?: string | null;
}

export interface BloqueioPayload {
  espacoId: number;
  data: string;
  horarioInicio: string;
  horarioFim: string;
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
