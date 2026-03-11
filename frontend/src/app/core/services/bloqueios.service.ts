import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Bloqueio, BloqueioPayload } from '../models';

@Injectable({
  providedIn: 'root'
})
export class BloqueiosService {
  private readonly http = inject(HttpClient);

  list(espacoId: number, data: string): Observable<Bloqueio[]> {
    const params = new HttpParams()
      .set('espacoId', espacoId)
      .set('data', data);
    return this.http.get<Bloqueio[]>(`${environment.apiUrl}/bloqueios`, { params });
  }

  create(payload: BloqueioPayload): Observable<Bloqueio> {
    return this.http.post<Bloqueio>(`${environment.apiUrl}/bloqueios`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/bloqueios/${id}`);
  }
}
