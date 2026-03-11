import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiErrorResponse } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ApiErrorService {
  toMessage(error: unknown, fallback = 'Nao foi possivel concluir a operacao.'): string {
    if (!(error instanceof HttpErrorResponse)) {
      return fallback;
    }

    if (error.status === 0) {
      return 'Nao foi possivel conectar com o backend.';
    }

    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }

    const payload = error.error as Partial<ApiErrorResponse> | null;
    if (payload?.message) {
      return payload.message;
    }

    if (payload?.error) {
      return payload.error;
    }

    return fallback;
  }
}
