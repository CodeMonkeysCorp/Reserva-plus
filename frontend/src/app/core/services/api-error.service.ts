import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiErrorResponse } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ApiErrorService {
  toMessage(error: unknown, fallback = 'Não foi possível concluir a operação.'): string {
    if (!(error instanceof HttpErrorResponse)) {
      return fallback;
    }

    if (error.status === 0) {
      return 'Não foi possível conectar com o backend.';
    }

    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }

    const payload = error.error as Partial<ApiErrorResponse> | null;
    if (payload?.message) {
      return payload.message;
    }

    if (payload?.fieldErrors) {
      const [firstError] = Object.values(payload.fieldErrors);
      if (firstError) {
        return firstError;
      }
    }

    if (payload?.error) {
      return payload.error;
    }

    return fallback;
  }
}
