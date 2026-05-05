import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AgendaDia, Reserva, ReservaCreatePayload } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ReservasService {
  private readonly http = inject(HttpClient);

  agenda(espacoId: number, data: string): Observable<AgendaDia> {
    return this.http.get<AgendaDia>(`${environment.apiUrl}/reservas/agenda`, {
      params: {
        espacoId,
        data
      }
    });
  }

  historico(data?: string): Observable<Reserva[]> {
    const options = data
      ? {
          params: {
            data
          }
        }
      : {};

    return this.http.get<Reserva[]>(`${environment.apiUrl}/reservas/historico`, options);
  }

  create(payload: ReservaCreatePayload): Observable<Reserva> {
    return this.http.post<Reserva>(`${environment.apiUrl}/reservas`, payload);
  }

  cancel(id: number): Observable<Reserva> {
    return this.http.post<Reserva>(`${environment.apiUrl}/reservas/${id}/cancelar`, {});
  }
}
