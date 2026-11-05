import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { MatchingService } from '../../core/matching.service';
import { ResultatMatching } from '../../core/models';

@Component({
  selector: 'app-recommendations-page',
  imports: [RouterLink],
  templateUrl: './recommendations-page.html',
  styleUrl: './recommendations-page.css',
})
export class RecommendationsPage {
  readonly auth = inject(AuthService);
  private matchingService = inject(MatchingService);

  readonly results = signal<ResultatMatching[] | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    if (this.auth.user()?.role === 'CANDIDAT') {
      this.loadRecommendations();
    }
  }

  loadRecommendations(): void {
    if (this.loading() || this.auth.user()?.role !== 'CANDIDAT') return;

    this.loading.set(true);
    this.error.set(null);
    this.results.set(null);

    this.matchingService.evaluer().pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: results => this.results.set(results),
      error: (error: HttpErrorResponse) => this.error.set(this.messageFor(error)),
    });
  }

  private messageFor(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Le service CV ne répond pas. Vérifiez que le cv-service fonctionne sur le port 8082.';
    }
    if (error.status === 401) {
      return 'Votre session a expiré. Reconnectez-vous pour consulter vos recommandations.';
    }
    if (error.status === 403) {
      return 'Cette page est réservée aux comptes candidats.';
    }
    if (error.status === 409) {
      return 'Validez d’abord vos compétences dans votre profil CV. Les recommandations seront disponibles ensuite.';
    }
    return error.error?.message ?? 'Impossible de calculer les recommandations. Réessayez.';
  }
}
