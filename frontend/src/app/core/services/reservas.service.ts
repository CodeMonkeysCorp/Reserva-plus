import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Reserva, ReservaCreatePayload } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ReservasService {
  private readonly http = inject(HttpClient);

  historico(): Observable<Reserva[]> {
    return this.http.get<Reserva[]>(`${environment.apiUrl}/reservas/historico`);
  }

  create(payload: ReservaCreatePayload): Observable<Reserva> {
    return this.http.post<Reserva>(`${environment.apiUrl}/reservas`, payload);
  }

  cancel(id: number): Observable<Reserva> {
    return this.http.post<Reserva>(`${environment.apiUrl}/reservas/${id}/cancelar`, {});
  }
}
