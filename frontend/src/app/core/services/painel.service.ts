import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PainelResumo } from '../models';

@Injectable({
  providedIn: 'root'
})
export class PainelService {
  private readonly http = inject(HttpClient);

  resumo(page = 0, size = 5): Observable<PainelResumo> {
    return this.http.get<PainelResumo>(`${environment.apiUrl}/painel/resumo`, {
      params: {
        page,
        size
      }
    });
  }
}
