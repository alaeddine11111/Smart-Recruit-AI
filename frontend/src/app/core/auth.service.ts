import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import {
  AuthResponse, LoginRequest, MessageResponse, RegisterCandidatRequest,
  RegisterRecruteurRequest, User,
} from './models';

const API = 'http://localhost:8081/api';
const TOKEN_KEY = 'sr_token';
const USER_KEY = 'sr_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  readonly user = signal<User | null>(this.readUser());
  readonly isLoggedIn = computed(() => this.user() !== null);

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  login(body: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/login`, body).pipe(tap(r => this.save(r)));
  }

  registerCandidat(body: RegisterCandidatRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/register/candidat`, body).pipe(tap(r => this.save(r)));
  }

  registerRecruteur(body: RegisterRecruteurRequest): Observable<MessageResponse> {
  return this.http.post<MessageResponse>(`${API}/auth/register/recruteur`, body);
}

  me(): Observable<User> {
    return this.http.get<User>(`${API}/auth/me`);
  }

  users(): Observable<User[]> {
    return this.http.get<User[]>(`${API}/admin/utilisateurs`);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.user.set(null);
    this.router.navigate(['/connexion']);
  }

  private save(r: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, r.token);
    localStorage.setItem(USER_KEY, JSON.stringify(r.user));
    this.user.set(r.user);
  }

  private readUser(): User | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}