import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface InterviewAnswerEvaluation {
  score: number;
  pertinence: string;
  exactitude: string;
  pointsManquants: string[];
  retour: string;
}

export interface InterviewQuestion {
  id: string;
  type: 'TECHNIQUE' | 'EXPERIENCE' | 'GENERALE' | string;
  question: string;
  reponse: string | null;
  evaluation: InterviewAnswerEvaluation | null;
}

export interface FinalInterviewEvaluation {
  forces: string[];
  faiblesses: string[];
  axesAmelioration: string[];
  conseils: string;
}

export interface PreparationSession {
  id: string;
  candidatId: string;
  offreId: string | null;
  offreTitre: string;
  dateCreation: string;
  dateFin: string | null;
  questions: InterviewQuestion[];
  scoreGlobal: number | null;
  evaluationFinale: FinalInterviewEvaluation | null;
  statut: 'EN_COURS' | 'TERMINEE';
}

@Injectable({ providedIn: 'root' })
export class InterviewPreparationService {
  private readonly http = inject(HttpClient);
  private readonly api = 'http://localhost:8082/api/preparations';

  history(): Observable<PreparationSession[]> {
    return this.http.get<PreparationSession[]>(this.api);
  }

  get(id: string): Observable<PreparationSession> {
    return this.http.get<PreparationSession>(`${this.api}/${encodeURIComponent(id)}`);
  }

  start(offreId: string | null): Observable<PreparationSession> {
    return this.http.post<PreparationSession>(this.api, { offreId });
  }

  submitAnswer(sessionId: string, questionId: string, reponse: string): Observable<PreparationSession> {
    return this.http.put<PreparationSession>(
      `${this.api}/${encodeURIComponent(sessionId)}/reponses`,
      { questionId, reponse },
    );
  }

  finish(sessionId: string): Observable<PreparationSession> {
    return this.http.post<PreparationSession>(
      `${this.api}/${encodeURIComponent(sessionId)}/terminer`,
      {},
    );
  }
}
