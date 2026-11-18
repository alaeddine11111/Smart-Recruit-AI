import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ApplicationStatus, Candidature, RecruiterApplicationStats, RecruiterCandidature,
} from './offers.models';

@Injectable({ providedIn: 'root' })
export class CandidatureService {
  private http = inject(HttpClient);
  private api = 'http://localhost:8084/api/candidatures';

  apply(offreId: string): Observable<Candidature> {
    return this.http.post<Candidature>(`${this.api}/${offreId}`, {});
  }

  mes(): Observable<Candidature[]> {
    return this.http.get<Candidature[]>(`${this.api}/mes`);
  }

  get(id: string): Observable<Candidature> {
    return this.http.get<Candidature>(`${this.api}/${id}`);
  }

  recruiterStats(): Observable<RecruiterApplicationStats> {
    return this.http.get<RecruiterApplicationStats>('http://localhost:8084/api/recruteur/candidatures/stats');
  }

  recruiterApplications(filters: {
    offreId?: string;
    statut?: ApplicationStatus | '';
    candidate?: string;
  } = {}): Observable<RecruiterCandidature[]> {
    let params = new HttpParams();
    if (filters.offreId) params = params.set('offreId', filters.offreId);
    if (filters.statut) params = params.set('statut', filters.statut);
    if (filters.candidate) params = params.set('candidate', filters.candidate);
    return this.http.get<RecruiterCandidature[]>(
      'http://localhost:8084/api/recruteur/candidatures', { params });
  }

  recruiterApplication(id: string): Observable<RecruiterCandidature> {
    return this.http.get<RecruiterCandidature>(
      `http://localhost:8084/api/recruteur/candidatures/${encodeURIComponent(id)}`);
  }

  updateRecruiterStatus(id: string, statut: ApplicationStatus): Observable<RecruiterCandidature> {
    return this.http.patch<RecruiterCandidature>(
      `http://localhost:8084/api/recruteur/candidatures/${encodeURIComponent(id)}/statut`, { statut });
  }
}
