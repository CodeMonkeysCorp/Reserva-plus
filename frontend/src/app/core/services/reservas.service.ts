import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AgendaDia, Reserva, ReservaCreatePayload } from '../models';

interface HistoricoReservaFiltros {
  data?: string;
  dataInicial?: string;
  dataFinal?: string;
}

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

  historico(filters?: HistoricoReservaFiltros): Observable<Reserva[]> {
    const params: Record<string, string> = {};

    if (filters?.data) {
      params['data'] = filters.data;
    }

    if (filters?.dataInicial) {
      params['dataInicial'] = filters.dataInicial;
    }

    if (filters?.dataFinal) {
      params['dataFinal'] = filters.dataFinal;
    }

    const options = Object.keys(params).length > 0 ? { params } : {};

    return this.http.get<Reserva[]>(`${environment.apiUrl}/reservas/historico`, options);
  }

  create(payload: ReservaCreatePayload): Observable<Reserva> {
    return this.http.post<Reserva>(`${environment.apiUrl}/reservas`, payload);
  }

  cancel(id: number): Observable<Reserva> {
    return this.http.post<Reserva>(`${environment.apiUrl}/reservas/${id}/cancelar`, {});
  }
}
