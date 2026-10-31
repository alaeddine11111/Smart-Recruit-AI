import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AnalyseInfo, Profil } from './models';

@Injectable({ providedIn: 'root' })
export class ProfilService {
  private http = inject(HttpClient);
  private base = 'http://localhost:8082/api';

  analyse() { return this.http.get<AnalyseInfo>(`${this.base}/cv/analyse`); }
  profil() { return this.http.get<Profil>(`${this.base}/profil`); }
  save(p: Profil) { return this.http.put<Profil>(`${this.base}/profil`, p); }
  valider() { return this.http.post<Profil>(`${this.base}/profil/valider`, {}); }
}