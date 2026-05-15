import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginPayload, RegisterPayload, UserRole } from '../models';

const SESSION_STORAGE_KEY = 'reserva_plus_session';

type SessionTokenClaims = {
  sub: string;
  uid: number;
  role: UserRole;
  exp: number;
};

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly sessionSubject = new BehaviorSubject<AuthResponse | null>(this.readStoredSession());
  private validationRequest$: Observable<AuthResponse | null> | null = null;
  private sessionValidated = false;

  readonly user$ = this.sessionSubject.asObservable();
  readonly isAuthenticated$ = this.user$.pipe(map((session) => session !== null));

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, payload).pipe(
      tap((session) => this.storeSession(session, true))
    );
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload).pipe(
      tap((session) => this.storeSession(session, true))
    );
  }

  validateSession(): Observable<AuthResponse | null> {
    const session = this.getActiveSession();
    if (!session) {
      return of(null);
    }

    if (this.sessionValidated) {
      return of(session);
    }

    if (this.validationRequest$) {
      return this.validationRequest$;
    }

    this.validationRequest$ = this.http.get<AuthResponse>(`${environment.apiUrl}/auth/me`).pipe(
      tap((validatedSession) => this.storeSession(validatedSession, true)),
      catchError(() => {
        this.clearSession();
        return of(null);
      }),
      finalize(() => {
        this.validationRequest$ = null;
        this.sessionValidated = this.sessionSubject.value !== null;
      }),
      shareReplay(1)
    );

    return this.validationRequest$;
  }

  logout(): void {
    this.clearSession();
  }

  isAuthenticated(): boolean {
    return this.getActiveSession() !== null;
  }

  isAdmin(): boolean {
    return this.getActiveSession()?.role === 'ADMIN';
  }

  get token(): string | null {
    return this.getActiveSession()?.token ?? null;
  }

  get user(): AuthResponse | null {
    return this.getActiveSession();
  }

  private storeSession(session: AuthResponse, validated: boolean): void {
    if (!this.isSessionStructurallyValid(session)) {
      this.clearSession();
      return;
    }

    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    this.sessionSubject.next(session);
    this.sessionValidated = validated;
  }

  private clearSession(): void {
    localStorage.removeItem(SESSION_STORAGE_KEY);
    this.sessionSubject.next(null);
    this.sessionValidated = false;
    this.validationRequest$ = null;
  }

  private getActiveSession(): AuthResponse | null {
    const session = this.sessionSubject.value;
    if (!session) {
      return null;
    }

    if (!this.isSessionStructurallyValid(session)) {
      this.clearSession();
      return null;
    }

    return session;
  }

  private readStoredSession(): AuthResponse | null {
    const rawSession = localStorage.getItem(SESSION_STORAGE_KEY);
    if (!rawSession) {
      return null;
    }

    try {
      const parsed = JSON.parse(rawSession) as Partial<AuthResponse>;
      if (!this.isSessionStructurallyValid(parsed)) {
        localStorage.removeItem(SESSION_STORAGE_KEY);
        return null;
      }

      return parsed;
    } catch {
      localStorage.removeItem(SESSION_STORAGE_KEY);
      return null;
    }
  }

  private isSessionStructurallyValid(session: Partial<AuthResponse> | null): session is AuthResponse {
    if (!session) {
      return false;
    }

    const role = session.role;
    const hasValidRole = role === 'ADMIN' || role === 'USER';
    if (
      typeof session.id !== 'number' ||
      typeof session.nome !== 'string' ||
      typeof session.email !== 'string' ||
      typeof session.token !== 'string' ||
      !hasValidRole
    ) {
      return false;
    }

    return this.matchesTokenClaims(
      {
        id: session.id,
        email: session.email,
        role
      },
      this.readTokenClaims(session.token)
    );
  }

  private matchesTokenClaims(
    session: Pick<AuthResponse, 'id' | 'email' | 'role'>,
    claims: SessionTokenClaims | null
  ): boolean {
    if (!claims) {
      return false;
    }

    return (
      claims.uid === session.id &&
      claims.role === session.role &&
      claims.sub.trim().toLowerCase() === session.email.trim().toLowerCase()
    );
  }

  private readTokenClaims(token: string): SessionTokenClaims | null {
    const payload = this.parseJwtPayload(token);
    if (!payload) {
      return null;
    }

    const sub = payload['sub'];
    const uid = payload['uid'];
    const role = payload['role'];
    const exp = payload['exp'];

    if (
      typeof sub !== 'string' ||
      typeof uid !== 'number' ||
      (role !== 'ADMIN' && role !== 'USER') ||
      typeof exp !== 'number'
    ) {
      return null;
    }

    if (exp * 1000 <= Date.now()) {
      return null;
    }

    return { sub, uid, role, exp };
  }

  private parseJwtPayload(token: string): Record<string, unknown> | null {
    const segments = token.split('.');
    if (segments.length !== 3 || typeof globalThis.atob !== 'function') {
      return null;
    }

    try {
      const base64 = segments[1].replace(/-/g, '+').replace(/_/g, '/');
      const padding = (4 - (base64.length % 4)) % 4;
      const normalized = base64 + '='.repeat(padding);
      return JSON.parse(globalThis.atob(normalized)) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}
