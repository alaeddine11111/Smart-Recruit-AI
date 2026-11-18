import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../../../core/auth.service';
import { CandidatureService } from '../../../core/candidature.service';
import { ApplicationStatus, OfferApplicationStats, RecruiterCandidature } from '../../../core/offers.models';

type ApplicationRow = RecruiterCandidature & { candidateName: string };

@Component({
  selector: 'app-recruiter-applications',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
  <section class="max-w-6xl mx-auto p-6">
    <a routerLink="/tableau-de-bord" class="text-blue-700">Back to dashboard</a>
    <h1 class="text-2xl font-semibold my-4">Applications</h1>

    <div class="grid sm:grid-cols-3 gap-3 mb-5">
      <label class="grid gap-1 text-sm">Job offer
        <select [value]="offerFilter()" (change)="setOfferFilter($event)" class="border rounded px-3 py-2">
          <option value="">All my offers</option>
          @for (offer of offers(); track offer.offreId) {
            <option [value]="offer.offreId">{{ offer.titre }}</option>
          }
        </select>
      </label>
      <label class="grid gap-1 text-sm">Status
        <select [value]="statusFilter()" (change)="setStatusFilter($event)" class="border rounded px-3 py-2">
          <option value="">All statuses</option>
          @for (status of statuses; track status) { <option [value]="status">{{ statusLabel(status) }}</option> }
        </select>
      </label>
      <label class="grid gap-1 text-sm">Candidate name or email
        <input [value]="candidateFilter()" (input)="candidateFilter.set($any($event.target).value)"
               placeholder="Search candidates" class="border rounded px-3 py-2" />
      </label>
    </div>

    @if (loading()) {
      <p class="border rounded p-4" role="status">Loading applications...</p>
    } @else if (error()) {
      <p class="border rounded p-4 text-red-700" role="alert">{{ error() }}</p>
    } @else if (filteredApplications().length === 0) {
      <p class="border rounded p-4 text-gray-600">
        {{ applications().length ? 'No applications match this candidate search.' : 'No applications received for your offers yet.' }}
      </p>
    } @else {
      <div class="overflow-x-auto border rounded">
        <table class="w-full text-left">
          <thead class="border-b bg-gray-50">
            <tr><th class="p-3">Candidate</th><th class="p-3">Email</th><th class="p-3">Offer</th><th class="p-3">Applied</th><th class="p-3">Status</th><th class="p-3"></th></tr>
          </thead>
          <tbody>
            @for (application of filteredApplications(); track application.id) {
              <tr class="border-b">
                <td class="p-3">{{ application.candidateName }}</td>
                <td class="p-3">{{ application.candidateEmail }}</td>
                <td class="p-3">{{ application.offre.titre }}</td>
                <td class="p-3">{{ application.dateCandidature | date:'dd/MM/yyyy HH:mm' }}</td>
                <td class="p-3"><span class="status" [class]="statusClass(application.statut)">{{ statusLabel(application.statut) }}</span></td>
                <td class="p-3"><a [routerLink]="[application.id]" class="text-blue-700">View details</a></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  </section>`,
  styles: `
    .status { display: inline-block; border-radius: 999px; padding: .25rem .65rem; font-size: .85rem; font-weight: 700; }
    .pending { background: #fff1b8; color: #705300; }
    .accepted { background: #d7f2df; color: #1f6734; }
    .rejected { background: #f7dce2; color: #8a2b3f; }
  `
})
export class ApplicationsListComponent implements OnInit {
  private readonly service = inject(CandidatureService);
  private readonly auth = inject(AuthService);
  readonly statuses: ApplicationStatus[] = ['PENDING', 'ACCEPTED', 'REJECTED'];
  readonly offers = signal<OfferApplicationStats[]>([]);
  readonly applications = signal<ApplicationRow[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly offerFilter = signal('');
  readonly statusFilter = signal<ApplicationStatus | ''>('');
  readonly candidateFilter = signal('');
  readonly filteredApplications = computed(() => {
    const query = this.candidateFilter().trim().toLowerCase();
    return this.applications().filter(application =>
      !query || `${application.candidateName} ${application.candidateEmail}`.toLowerCase().includes(query));
  });

  ngOnInit() {
    this.service.recruiterStats().subscribe({
      next: stats => this.offers.set(stats.offers),
      error: () => this.error.set('Unable to load your offers and application filters.')
    });
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.service.recruiterApplications({
      offreId: this.offerFilter(),
      statut: this.statusFilter()
    }).subscribe({
      next: applications => {
        if (applications.length === 0) {
          this.applications.set([]);
          this.loading.set(false);
          return;
        }
        forkJoin(applications.map(application =>
          this.auth.recruiterCandidate(application.id).pipe(
            map(candidate => ({ ...application, candidateName: candidate.nom })),
            catchError(() => of({ ...application, candidateName: 'Name unavailable' }))
          )
        )).subscribe(rows => {
          this.applications.set(rows);
          this.loading.set(false);
        });
      },
      error: () => {
        this.error.set('Unable to load applications. Please try again.');
        this.loading.set(false);
      }
    });
  }

  setOfferFilter(event: Event) {
    this.offerFilter.set((event.target as HTMLSelectElement).value);
    this.load();
  }

  setStatusFilter(event: Event) {
    this.statusFilter.set((event.target as HTMLSelectElement).value as ApplicationStatus | '');
    this.load();
  }

  statusLabel(status: ApplicationStatus): string {
    return status === 'PENDING' ? 'Pending' : status === 'ACCEPTED' ? 'Accepted' : 'Rejected';
  }

  statusClass(status: ApplicationStatus): string {
    return status.toLowerCase();
  }
}
