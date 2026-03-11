import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Espaco, EspacoPayload } from '../models';

@Injectable({
  providedIn: 'root'
})
export class EspacosService {
  private readonly http = inject(HttpClient);

  list(): Observable<Espaco[]> {
    return this.http.get<Espaco[]>(`${environment.apiUrl}/espacos`);
  }

  create(payload: EspacoPayload): Observable<Espaco> {
    return this.http.post<Espaco>(`${environment.apiUrl}/espacos`, payload);
  }

  update(id: number, payload: EspacoPayload): Observable<Espaco> {
    return this.http.put<Espaco>(`${environment.apiUrl}/espacos/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/espacos/${id}`);
  }
}
