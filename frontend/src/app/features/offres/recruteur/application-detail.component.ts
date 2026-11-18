import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../../../core/auth.service';
import { CandidatureService } from '../../../core/candidature.service';
import { CvService } from '../../../core/cv.service';
import { ProfilService } from '../../../core/profil.service';
import { CvInfo, Profil, RecruiterCandidate } from '../../../core/models';
import { ApplicationStatus, RecruiterCandidature } from '../../../core/offers.models';

@Component({
  selector: 'app-recruiter-application-detail',
  standalone: true,
  imports: [DatePipe, DecimalPipe, RouterLink],
  template: `
  <section class="max-w-4xl mx-auto p-6">
    <a routerLink="/recruteur/candidatures" class="text-blue-700">Back to applications</a>
    @if (loading()) {
      <p class="border rounded p-4 mt-4" role="status">Loading application details...</p>
    } @else if (error()) {
      <p class="border rounded p-4 mt-4 text-red-700" role="alert">{{ error() }}</p>
    } @else if (application(); as item) {
      <h1 class="text-2xl font-semibold mt-4">{{ item.offre.titre }}</h1>
      <p class="text-gray-600">Applied {{ item.dateCandidature | date:'dd/MM/yyyy HH:mm' }}</p>
      <p class="mt-2">Status: <strong>{{ statusLabel(item.statut) }}</strong></p>

      <section class="card">
        <h2>Candidate</h2>
        <dl>
          <div><dt>Name</dt><dd>
            @if (candidate()?.nom; as name) { {{ name }} }
            @else if (candidateError()) { {{ candidateError() }} }
            @else { Name not provided. }
          </dd></div>
          <div><dt>Email</dt><dd>{{ candidate()?.email || item.candidateEmail }}</dd></div>
          <div><dt>Phone</dt><dd>{{ contactValue(candidate()?.telephone, profile()?.telephone) }}</dd></div>
          <div><dt>Location</dt><dd>{{ contactValue(candidate()?.localisation, profile()?.localisation) }}</dd></div>
        </dl>
        @if (candidateError()) { <p class="data-error" role="alert">{{ candidateError() }}</p> }
      </section>

      <section class="card">
        <h2>Validated skills</h2>
        @if (profileError()) { <p class="data-error" role="alert">{{ profileError() }}</p> }
        @else {
          <div class="chips">
            @for (skill of profile()?.competencesValidees ?? []; track $index) {
              <span class="chip">{{ skill.nom }}{{ skill.categorie ? ' · ' + skill.categorie : '' }}</span>
            } @empty { <p>{{ profile() ? 'No validated skills saved.' : profileMissing() ? 'No profile record exists for this candidate.' : 'No validated skills saved.' }}</p> }
          </div>
        }
      </section>

      <section class="card">
        <h2>Education and training</h2>
        @if (profileError()) { <p class="data-error" role="alert">{{ profileError() }}</p> }
        @else {
          @for (formation of profile()?.formations ?? []; track $index) {
            <article class="record">
              <strong>{{ formation.diplome || 'Degree not specified' }}</strong>
              <p>{{ formation.etablissement || 'Institution not specified' }} · {{ formation.dateDebut }} – {{ formation.dateFin }}</p>
            </article>
          } @empty { <p>{{ profile() ? 'No education information provided.' : profileMissing() ? 'No profile record exists for this candidate.' : 'No education information provided.' }}</p> }
        }
      </section>

      <section class="card">
        <h2>Professional experience</h2>
        @if (profileError()) { <p class="data-error" role="alert">{{ profileError() }}</p> }
        @else {
          @for (experience of profile()?.experiences ?? []; track $index) {
            <article class="record">
              <strong>{{ experience.poste || 'Position not specified' }}</strong>
              <p>{{ experience.entreprise || 'Company not specified' }} · {{ experience.dateDebut }} – {{ experience.dateFin }}</p>
              @if (experience.description) { <p>{{ experience.description }}</p> }
            </article>
          } @empty { <p>{{ profile() ? 'No professional experience provided.' : profileMissing() ? 'No profile record exists for this candidate.' : 'No professional experience provided.' }}</p> }
        }
      </section>

      <section class="card">
        <h2>CV</h2>
        @if (cvError()) {
          <p class="data-error" role="alert">{{ cvError() }}</p>
        } @else if (cv(); as info) {
          <p>{{ info.nomFichier }} · {{ (info.taille / 1024) | number:'1.0-0' }} KB · uploaded {{ info.dateDepot | date:'dd/MM/yyyy' }}</p>
        } @else { <p>No CV is on file for this candidate.</p> }
      </section>

      <section class="card">
        <h2>Applied offer</h2>
        <p>{{ item.offre.titre }} · {{ item.offre.entrepriseNom }}</p>
        <p>{{ item.offre.localisation }} · {{ item.offre.typeContrat }}</p>
      </section>

      @if (message()) {
        <p class="mt-4" [class.text-green-700]="!actionError()" [class.text-red-700]="actionError()" role="status">{{ message() }}</p>
      }
      <div class="actions">
        <button type="button" (click)="setStatus('PENDING')" [disabled]="saving()" class="border rounded px-4 py-2">Keep pending</button>
        <button type="button" (click)="setStatus('ACCEPTED')" [disabled]="saving()" class="rounded px-4 py-2 bg-green-700 text-white">Accept</button>
        <button type="button" (click)="setStatus('REJECTED')" [disabled]="saving()" class="rounded px-4 py-2 bg-red-700 text-white">Reject</button>
      </div>
    }
  </section>`,
  styles: `
    .card { margin-top: 1.25rem; padding: 1rem; border: 1px solid #ccd3df; border-radius: 10px; background: white; }
    .card h2 { margin: 0 0 .75rem; font-size: 1.15rem; font-weight: 600; }
    dl { display: grid; grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr)); gap: .75rem 1rem; }
    dt { color: #4a5877; font-size: .9rem; }
    dd { margin: .15rem 0 0; overflow-wrap: anywhere; font-weight: 600; }
    .chips, .actions { display: flex; flex-wrap: wrap; gap: .5rem; }
    .chip { padding: .25rem .65rem; border: 1px solid #ccd3df; border-radius: 999px; }
    .record { padding: .65rem 0; border-bottom: 1px solid #e4e8ef; }
    .record:last-child { border-bottom: 0; }
    .record p { margin: .25rem 0 0; }
    .actions { margin-top: 1.25rem; }
    .data-error { margin-top: .65rem; color: #a1233a; }
  `
})
export class ApplicationDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly applications = inject(CandidatureService);
  private readonly auth = inject(AuthService);
  private readonly profiles = inject(ProfilService);
  private readonly cvs = inject(CvService);

  readonly application = signal<RecruiterCandidature | null>(null);
  readonly candidate = signal<RecruiterCandidate | null>(null);
  readonly profile = signal<Profil | null>(null);
  readonly cv = signal<CvInfo | null>(null);
  readonly candidateError = signal('');
  readonly profileError = signal('');
  readonly cvError = signal('');
  readonly profileMissing = signal(false);
  readonly candidateRequestFailed = signal(false);
  readonly profileRequestFailed = signal(false);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly actionError = signal(false);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Application not found.');
      this.loading.set(false);
      return;
    }
    this.applications.recruiterApplication(id).subscribe({
      next: application => {
        this.application.set(application);
        forkJoin({
          candidate: this.loadResult(this.auth.recruiterCandidate(id)),
          profile: this.loadResult(this.profiles.forApplication(id)),
          cv: this.loadResult(this.cvs.forApplication(id))
        }).subscribe(result => {
          this.candidate.set(result.candidate.value);
          this.profile.set(result.profile.value);
          this.cv.set(result.cv.value);
          this.candidateError.set(this.errorMessage(result.candidate.error, 'candidate contact'));
          this.profileError.set(this.errorMessage(result.profile.error, 'candidate profile'));
          this.cvError.set(this.errorMessage(result.cv.error, 'candidate CV'));
          this.profileMissing.set(result.profile.error?.status === 404);
          this.candidateRequestFailed.set(!!result.candidate.error && result.candidate.error.status !== 404);
          this.profileRequestFailed.set(!!result.profile.error && result.profile.error.status !== 404);
          this.loading.set(false);
        });
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status === 403 || error.status === 404
          ? 'You cannot view this application.'
          : 'Unable to load application details. Please try again.');
        this.loading.set(false);
      }
    });
  }

  setStatus(statut: ApplicationStatus) {
    const application = this.application();
    if (!application || this.saving() || application.statut === statut) return;
    const action = statut === 'ACCEPTED' ? 'accept' : statut === 'REJECTED' ? 'reject' : 'keep pending';
    if (!confirm(`Are you sure you want to ${action} this application?`)) return;

    this.saving.set(true);
    this.message.set('');
    this.applications.updateRecruiterStatus(application.id, statut).subscribe({
      next: updated => {
        this.application.set(updated);
        this.message.set(`Application marked ${this.statusLabel(statut).toLowerCase()}.`);
        this.actionError.set(false);
        this.saving.set(false);
      },
      error: () => {
        this.message.set('Could not update this application. You may not own its offer.');
        this.actionError.set(true);
        this.saving.set(false);
      }
    });
  }

  statusLabel(status: ApplicationStatus): string {
    return status === 'PENDING' ? 'Pending' : status === 'ACCEPTED' ? 'Accepted' : 'Rejected';
  }

  contactValue(primary: string | null | undefined, secondary: string | null | undefined): string {
    if (primary?.trim()) return primary;
    if (secondary?.trim()) return secondary;
    if (this.candidateRequestFailed()) return 'Could not load candidate contact information.';
    if (this.profileRequestFailed()) return 'Could not load candidate profile information.';
    return 'Not provided';
  }

  private loadResult<T>(request: Observable<T>): Observable<{ value: T | null; error: HttpErrorResponse | null }> {
    return request.pipe(
      map(value => ({ value, error: null })),
      catchError((error: HttpErrorResponse) => of({ value: null, error }))
    );
  }

  private errorMessage(error: HttpErrorResponse | null, dataName: string): string {
    if (!error) return '';
    if (error.status === 404) return dataName === 'candidate contact'
      ? 'Candidate account details were not found.'
      : '';
    if (error.status === 401) return `Your session expired while loading ${dataName}. Please sign in again.`;
    if (error.status === 403) return `You are not authorized to load ${dataName} for this application.`;
    if (error.status === 0) return `The service for ${dataName} could not be reached. Check that the backend service is running.`;
    return `Could not load ${dataName} (HTTP ${error.status}). Please try again.`;
  }
}
