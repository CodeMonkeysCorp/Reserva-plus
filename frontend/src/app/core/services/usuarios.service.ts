import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminUser, AdminUserUpdatePayload } from '../models';

@Injectable({
  providedIn: 'root'
})
export class UsuariosService {
  private readonly http = inject(HttpClient);

  list(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${environment.apiUrl}/usuarios`);
  }

  update(id: number, payload: AdminUserUpdatePayload): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${environment.apiUrl}/usuarios/${id}`, payload);
  }
}
