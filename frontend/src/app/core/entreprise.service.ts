import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Entreprise } from './offers.models';

/** JWT is attached by your existing auth interceptor. */
@Injectable({ providedIn: 'root' })
export class EntrepriseService {
  private http = inject(HttpClient);
  private api = 'http://localhost:8084/api';

  getMine(): Observable<Entreprise> { return this.http.get<Entreprise>(`${this.api}/recruteur/entreprise`); }
  saveMine(e: Entreprise): Observable<Entreprise> { return this.http.put<Entreprise>(`${this.api}/recruteur/entreprise`, e); }
  getById(id: string): Observable<Entreprise> { return this.http.get<Entreprise>(`${this.api}/entreprises/${id}`); }
}
