import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, BehaviorSubject, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginPayload, RegisterPayload } from '../models';

const SESSION_STORAGE_KEY = 'reserva_plus_session';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly sessionSubject = new BehaviorSubject<AuthResponse | null>(this.readStoredSession());
  readonly user$ = this.sessionSubject.asObservable();
  readonly isAuthenticated$ = this.user$.pipe(map((session) => session !== null));

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, payload).pipe(
      tap((session) => this.storeSession(session))
    );
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload).pipe(
      tap((session) => this.storeSession(session))
    );
  }

  logout(): void {
    localStorage.removeItem(SESSION_STORAGE_KEY);
    this.sessionSubject.next(null);
  }

  isAuthenticated(): boolean {
    return this.sessionSubject.value !== null;
  }

  isAdmin(): boolean {
    return this.sessionSubject.value?.role === 'ADMIN';
  }

  get token(): string | null {
    return this.sessionSubject.value?.token ?? null;
  }

  get user(): AuthResponse | null {
    return this.sessionSubject.value;
  }

  private storeSession(session: AuthResponse): void {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    this.sessionSubject.next(session);
  }

  private readStoredSession(): AuthResponse | null {
    const rawSession = localStorage.getItem(SESSION_STORAGE_KEY);
    if (!rawSession) {
      return null;
    }

    try {
      const parsed = JSON.parse(rawSession) as Partial<AuthResponse>;
      const hasValidRole = parsed.role === 'ADMIN' || parsed.role === 'USER';
      const isValid =
        typeof parsed.id === 'number' &&
        typeof parsed.nome === 'string' &&
        typeof parsed.email === 'string' &&
        typeof parsed.token === 'string' &&
        hasValidRole;

      if (!isValid) {
        localStorage.removeItem(SESSION_STORAGE_KEY);
        return null;
      }

      return parsed as AuthResponse;
    } catch {
      localStorage.removeItem(SESSION_STORAGE_KEY);
      return null;
    }
  }
}
