import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CandidatureService } from '../../core/candidature.service';
import { OffreExterneService } from '../../core/offre-externe.service';
import { Candidature, CandidatureExterne } from '../../core/offers.models';

@Component({
  selector: 'app-applications',
  standalone: true,
  imports: [RouterLink, DatePipe],
  template: `
  <section class="applications-page">
    <a routerLink="/offres" class="back-link">Back to offers</a>
    <h1>My applications</h1>

    <section aria-labelledby="internal-applications-heading">
      <h2 id="internal-applications-heading">Internal Applications</h2>
      @if (internalLoading()) {
        <p class="state" role="status">Loading internal applications...</p>
      } @else if (internalError()) {
        <p class="state error" role="alert">{{ internalError() }}</p>
      } @else if (applications().length === 0) {
        <p class="state">You have no internal applications.</p>
      } @else {
      <div class="application-list">
        @for (application of applications(); track application.id) {
          <article class="application-card">
            <div>
              <h2>{{ application.offre?.titre || 'Offer unavailable' }}</h2>
              <p class="meta">
                {{ application.offre?.entrepriseNom || 'Company unavailable' }}
                @if (application.offre?.localisation) { · {{ application.offre?.localisation }} }
              </p>
              <p class="date">Applied {{ application.dateCandidature | date:'dd/MM/yyyy HH:mm' }}</p>
            </div>
            <div class="actions">
              <span class="status" [class.pending]="application.statut === 'PENDING'"
                    [class.accepted]="application.statut === 'ACCEPTED'"
                    [class.rejected]="application.statut === 'REJECTED'">
                {{ statusLabel(application.statut) }}
              </span>
              @if (application.offre) {
                <a [routerLink]="['/offres', application.offreId]" class="offer-link">View offer</a>
              }
            </div>
          </article>
        }
      </div>
      }
    </section>

    <section aria-labelledby="external-applications-heading">
      <h2 id="external-applications-heading">External Applications</h2>
      @if (externalLoading()) {
        <p class="state" role="status">Loading external application history...</p>
      } @else if (externalError()) {
        <p class="state error" role="alert">{{ externalError() }}</p>
      } @else if (externalApplications().length === 0) {
        <p class="state">You have no external offer redirects yet.</p>
      } @else {
        <div class="application-list">
          @for (application of externalApplications(); track application.id) {
            <article class="application-card">
              <div>
                <h3>{{ application.titre }}</h3>
                <p class="meta">{{ application.entreprise || 'Company not specified' }} · via {{ application.source }}</p>
                <p class="date">Link opened {{ application.dateClic | date:'dd/MM/yyyy HH:mm' }}</p>
              </div>
              <div class="actions">
                <span class="status redirected">Redirected</span>
                <a [href]="application.urlExterne" target="_blank" rel="noopener noreferrer" class="offer-link">
                  Open original application page
                </a>
              </div>
            </article>
          }
        </div>
      }
    </section>
  </section>`,
  styles: `
    .applications-page { max-width: 56rem; margin: 0 auto; padding: 2.5rem 2rem; }
    .back-link, .offer-link { color: #1d43c4; text-decoration: none; }
    h1 { margin: 1rem 0 1.25rem; }
    section + section { margin-top: 2rem; }
    section h2 { margin: 0 0 0.75rem; font-size: 1.25rem; }
    .state { padding: 1rem; border: 1px solid #ccd3df; border-radius: 10px; background: #fff; }
    .error { color: #a1233a; }
    .application-list { display: grid; gap: 0.75rem; }
    .application-card { display: flex; justify-content: space-between; gap: 1rem; padding: 1rem; background: #fff; border: 1px solid #ccd3df; border-radius: 10px; }
    .application-card h2 { margin: 0; font-size: 1.05rem; }
    .application-card h3 { margin: 0; font-size: 1.05rem; }
    .meta, .date { margin: 0.35rem 0 0; color: #4a5877; }
    .actions { display: flex; align-items: flex-end; flex-direction: column; gap: 0.6rem; }
    .status { padding: 0.35rem 0.65rem; border-radius: 999px; font-weight: 700; font-size: 0.85rem; }
    .pending { background: #fff1b8; color: #705300; }
    .accepted { background: #d7f2df; color: #1f6734; }
    .rejected { background: #f7dce2; color: #8a2b3f; }
    .redirected { background: #e2e8f4; color: #3e4d6c; }
    @media (max-width: 600px) { .application-card { flex-direction: column; } .actions { align-items: flex-start; } }
  `
})
export class ApplicationsPage implements OnInit {
  private candidatures = inject(CandidatureService);
  private externalCandidatures = inject(OffreExterneService);
  applications = signal<Candidature[]>([]);
  externalApplications = signal<CandidatureExterne[]>([]);
  internalLoading = signal(true);
  externalLoading = signal(true);
  internalError = signal('');
  externalError = signal('');

  ngOnInit() {
    this.candidatures.mes().subscribe({
      next: applications => {
        this.applications.set(applications);
        this.internalLoading.set(false);
      },
      error: () => {
        this.internalError.set('Unable to load internal applications. Please try again.');
        this.internalLoading.set(false);
      }
    });
    this.externalCandidatures.myExternalApplications().subscribe({
      next: applications => {
        this.externalApplications.set(applications);
        this.externalLoading.set(false);
      },
      error: () => {
        this.externalError.set('Unable to load external application history. Please try again.');
        this.externalLoading.set(false);
      }
    });
  }

  statusLabel(status: Candidature['statut']): string {
    if (status === 'ACCEPTED') return 'Accepted';
    if (status === 'REJECTED') return 'Rejected';
    return 'Pending';
  }
}
