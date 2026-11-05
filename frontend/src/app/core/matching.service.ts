import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OffreMatching, ResultatMatching } from './models';

const API = 'http://localhost:8082/api';

export const SAMPLE_OFFERS: OffreMatching[] = [
  {
    id: 'sample-java-developer',
    titre: 'Développeur Java junior',
    source: 'EXEMPLE',
    competencesRequises: ['Java', 'Spring Boot', 'SQL', 'Git'],
  },
  {
    id: 'sample-frontend-developer',
    titre: 'Développeur front-end',
    source: 'EXEMPLE',
    competencesRequises: ['Angular', 'TypeScript', 'HTML', 'CSS'],
  },
  {
    id: 'sample-full-stack-developer',
    titre: 'Développeur full-stack',
    source: 'EXEMPLE',
    competencesRequises: ['Java', 'Angular', 'REST', 'MongoDB', 'Git'],
  },
];

@Injectable({ providedIn: 'root' })
export class MatchingService {
  private http = inject(HttpClient);

  evaluer(offres: OffreMatching[] = SAMPLE_OFFERS): Observable<ResultatMatching[]> {
    return this.http.post<ResultatMatching[]>(`${API}/matching`, offres);
  }
}
