import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import {
  InterviewQuestion,
  InterviewPreparationService,
  PreparationSession,
} from '../../core/interview-preparation.service';
import { OffreService } from '../../core/offre.service';
import { OffreEmploi } from '../../core/offers.models';

@Component({
  selector: 'app-interview-preparation-page',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './interview-preparation-page.html',
  styleUrl: './interview-preparation-page.css',
})
export class InterviewPreparationPage {
  readonly auth = inject(AuthService);
  private readonly preparationService = inject(InterviewPreparationService);
  private readonly offerService = inject(OffreService);

  readonly sessions = signal<PreparationSession[]>([]);
  readonly offers = signal<OffreEmploi[]>([]);
  readonly currentSession = signal<PreparationSession | null>(null);
  readonly lastEvaluatedQuestion = signal<InterviewQuestion | null>(null);
  readonly questionIndex = signal(0);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly selectedOfferId = signal('');
  answer = '';

  constructor() {
    this.loadHistory();
    this.offerService.search({}, 0, 100).subscribe({
      next: page => this.offers.set(page.content),
      error: () => this.error.set('La liste des offres est indisponible. Vous pouvez tout de même commencer un entretien général.'),
    });
  }

  start(): void {
    this.loading.set(true);
    this.error.set(null);
    this.preparationService.start(this.selectedOfferId() || null).subscribe({
      next: session => {
        this.setCurrentSession(session);
        this.loadHistory();
        this.loading.set(false);
      },
      error: error => {
        this.error.set(this.messageFor(error));
        this.loading.set(false);
      },
    });
  }

  openSession(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.preparationService.get(id).subscribe({
      next: session => {
        this.setCurrentSession(session);
        this.loading.set(false);
      },
      error: error => {
        this.error.set(this.messageFor(error));
        this.loading.set(false);
      },
    });
  }

  submitAnswer(): void {
    const session = this.currentSession();
    const question = session?.questions[this.questionIndex()];
    const answer = this.answer.trim();
    if (!session || !question || !answer || this.saving()) return;

    this.saving.set(true);
    this.error.set(null);
    this.preparationService.submitAnswer(session.id, question.id, answer).subscribe({
      next: updated => {
        this.setCurrentSession(updated);
        this.lastEvaluatedQuestion.set(
          updated.questions.find(item => item.id === question.id) ?? null,
        );
        this.loadHistory();
        this.saving.set(false);
      },
      error: error => {
        this.error.set(this.messageFor(error));
        this.saving.set(false);
      },
    });
  }

  finish(): void {
    const session = this.currentSession();
    if (!session || this.saving()) return;

    this.saving.set(true);
    this.error.set(null);
    this.preparationService.finish(session.id).subscribe({
      next: updated => {
        this.setCurrentSession(updated);
        this.loadHistory();
        this.saving.set(false);
      },
      error: error => {
        this.error.set(this.messageFor(error));
        this.saving.set(false);
      },
    });
  }

  backToHome(): void {
    this.currentSession.set(null);
    this.error.set(null);
  }

  answeredCount(session = this.currentSession()): number {
    return session?.questions.filter(question => question.evaluation !== null).length ?? 0;
  }

  private loadHistory(): void {
    this.preparationService.history().subscribe({
      next: sessions => this.sessions.set(sessions),
      error: error => this.error.set(this.messageFor(error)),
    });
  }

  private setCurrentSession(session: PreparationSession): void {
    this.currentSession.set(session);
    const firstUnanswered = session.questions.findIndex(question => question.evaluation === null);
    this.questionIndex.set(firstUnanswered < 0 ? session.questions.length : firstUnanswered);
    this.lastEvaluatedQuestion.set(
      session.questions.slice(0, firstUnanswered < 0 ? session.questions.length : firstUnanswered)
        .filter(question => question.evaluation !== null).at(-1) ?? null,
    );
    this.answer = '';
  }

  private messageFor(error: HttpErrorResponse): string {
    if (error.status === 0) return 'Impossible de joindre le service de préparation ou le service IA. Vérifiez qu’ils sont lancés.';
    if (error.status === 401) return 'Votre session a expiré. Reconnectez-vous.';
    if (error.status === 403) return 'Cette page est réservée aux comptes candidats.';
    if (error.status === 404) return 'Cette session ou cette offre est introuvable.';
    if (error.status === 400) return 'Complétez votre profil et validez au moins une compétence avant de démarrer la préparation.';
    if (error.status === 502 || error.status === 503) return 'Le service IA est momentanément indisponible. Votre progression enregistrée est conservée.';
    return error.error?.message ?? 'Une erreur est survenue. Réessayez.';
  }
}
