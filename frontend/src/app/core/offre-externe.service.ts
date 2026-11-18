import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CandidatureExterne, CandidatureExterneRequest, ExternalOffre } from './offers.models';

@Injectable({ providedIn: 'root' })
export class OffreExterneService {
  private http = inject(HttpClient);
  private api = 'http://localhost:8084/api';

  search(q: string, location: string, page = 1): Observable<ExternalOffre[]> {
    const params = new HttpParams().set('q', q).set('location', location).set('page', page);
    return this.http.get<ExternalOffre[]>(`${this.api}/offres-externes`, { params });
  }

  trackApplication(request: CandidatureExterneRequest): Observable<CandidatureExterne> {
    return this.http.post<CandidatureExterne>(`${this.api}/candidatures/externes`, request);
  }

  myExternalApplications(): Observable<CandidatureExterne[]> {
    return this.http.get<CandidatureExterne[]>(`${this.api}/candidatures/externes`);
  }
}
