import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EMPTY, Observable, catchError, expand, finalize, map, of, reduce, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { MatchingService } from '../../core/matching.service';
import { OffreExterneService } from '../../core/offre-externe.service';
import { OffreService } from '../../core/offre.service';
import { ProfilService } from '../../core/profil.service';
import { OffreEmploi, PageResponse } from '../../core/offers.models';
import { Competence, OffreMatching, Profil, ResultatMatching } from '../../core/models';

@Component({
  selector: 'app-recommendations-page',
  imports: [RouterLink],
  templateUrl: './recommendations-page.html',
  styleUrl: './recommendations-page.css',
})
export class RecommendationsPage {
  readonly auth = inject(AuthService);
  private matchingService = inject(MatchingService);
  private readonly profileService = inject(ProfilService);
  private readonly offerService = inject(OffreService);
  private readonly externalOffers = inject(OffreExterneService);

  readonly results = signal<ResultatMatching[] | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly externalNotice = signal<string | null>(null);
  readonly unscoredExternalCount = signal(0);

  constructor() {
    if (this.auth.user()?.role === 'CANDIDAT') {
      this.loadRecommendations();
    }
  }

  loadRecommendations(): void {
    if (this.loading() || this.auth.user()?.role !== 'CANDIDAT') return;

    this.loading.set(true);
    this.error.set(null);
    this.externalNotice.set(null);
    this.unscoredExternalCount.set(0);
    this.results.set(null);

    this.profileService.profil().pipe(
      switchMap(profile => {
        if (!profile.valide) {
          this.error.set('Validez d’abord vos compétences dans Mon CV. Les recommandations seront disponibles ensuite.');
          return EMPTY;
        }
        if (!profile.competencesValidees?.length) {
          this.error.set('Sélectionnez et enregistrez au moins une compétence dans Mon CV pour obtenir des recommandations.');
          return EMPTY;
        }

        return this.loadPublishedOffers().pipe(
          switchMap(internalOffers => this.loadExternalOffers(profile).pipe(
            map(externalOffers => [
              ...internalOffers.map(offer => this.matchingService.internalOffer(offer)),
              ...externalOffers,
            ]),
          )),
          switchMap(offers => offers.length ? this.matchingService.evaluer(offers) : of([])),
        );
      }),
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: results => this.results.set(results),
      error: (error: HttpErrorResponse) => this.error.set(this.messageFor(error)),
    });
  }

  private loadPublishedOffers(): Observable<OffreEmploi[]> {
    const firstPage = 0;
    const pageSize = 100;
    return this.offerService.search({}, firstPage, pageSize).pipe(
      expand((page: PageResponse<OffreEmploi>) =>
        page.page + 1 < page.totalPages
          ? this.offerService.search({}, page.page + 1, pageSize)
          : EMPTY,
      ),
      reduce((offers, page) => offers.concat(page.content), [] as OffreEmploi[]),
    );
  }

  private loadExternalOffers(profile: Profil): Observable<OffreMatching[]> {
    const skills = (profile.competencesValidees ?? [])
      .map((skill: Competence) => skill.nom.trim())
      .filter(Boolean);
    if (skills.length === 0) {
      return of([]);
    }

    return this.externalOffers.search(skills.slice(0, 5).join(' '), profile.localisation ?? '').pipe(
      map(external => {
        const offers = external
          .map(offer => this.matchingService.externalOffer(offer))
          .filter((offer): offer is OffreMatching => offer !== null);
        const unscoredCount = external.length - offers.length;
        if (unscoredCount > 0) {
          this.unscoredExternalCount.set(unscoredCount);
        }
        return offers;
      }),
      catchError(() => {
        this.externalNotice.set('Les offres externes sont temporairement indisponibles. Les offres internes restent affichées.');
        return of([]);
      }),
    );
  }

  private messageFor(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Impossible de joindre un service nécessaire. Vérifiez que le CV service (8082) et le service des offres (8084) sont lancés.';
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
