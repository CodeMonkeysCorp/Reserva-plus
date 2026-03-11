export type UserRole = 'ADMIN' | 'USER';
export type EspacoTipo = 'QUADRA' | 'QUIOSQUE';
export type ReservaStatus = 'ATIVA' | 'CANCELADA';

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

export interface Espaco {
  id: number;
  nome: string;
  tipo: EspacoTipo;
  descricao?: string;
  ativo: boolean;
}

export interface EspacoPayload {
  nome: string;
  tipo: EspacoTipo;
  descricao?: string;
  ativo: boolean;
}

export interface Reserva {
  id: number;
  usuarioId: number;
  usuarioNome: string;
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

export interface Bloqueio {
  id: number;
  espacoId: number;
  espacoNome: string;
  data: string;
  horarioInicio: string;
  horarioFim: string;
  motivo?: string;
}

export interface BloqueioPayload {
  espacoId: number;
  data: string;
  horarioInicio: string;
  horarioFim: string;
  motivo?: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
