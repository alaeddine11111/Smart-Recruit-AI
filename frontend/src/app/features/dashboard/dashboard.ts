import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ProfilService } from '../../core/profil.service';
import { Profil, User } from '../../core/models';
import { CandidatureService } from '../../core/candidature.service';
import { RecruiterApplicationStats } from '../../core/offers.models';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  readonly auth = inject(AuthService);
  readonly user = this.auth.user;
  private readonly profilService = inject(ProfilService);
  private readonly candidatureService = inject(CandidatureService);
  private readonly destroyRef = inject(DestroyRef);

  readonly me = signal<User | null>(null);
  readonly users = signal<User[] | null>(null);
  readonly adminError = signal<string | null>(null);
  readonly candidateProfile = signal<Profil | null>(null);
  readonly candidateLoading = signal(false);
  readonly candidateError = signal<string | null>(null);
  readonly recruiterStats = signal<RecruiterApplicationStats | null>(null);
  readonly recruiterStatsLoading = signal(false);
  readonly recruiterStatsError = signal<string | null>(null);

  constructor() {
    const role = this.user()?.role;
    if (role === 'CANDIDAT') {
      this.candidateLoading.set(true);
      forkJoin({
        user: this.auth.me(),
        profile: this.profilService.profil(),
      }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: ({ user, profile }) => {
          this.me.set(user);
          this.candidateProfile.set(profile);
          this.candidateLoading.set(false);
        },
        error: () => {
          this.candidateError.set('Impossible de charger votre profil. Réessayez plus tard.');
          this.candidateLoading.set(false);
        },
      });
    } else {
      this.auth.me().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: u => this.me.set(u),
      });
    }

    if (role === 'RECRUTEUR') {
      this.recruiterStatsLoading.set(true);
      this.candidatureService.recruiterStats().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: stats => {
          this.recruiterStats.set(stats);
          this.recruiterStatsLoading.set(false);
        },
        error: () => {
          this.recruiterStatsError.set('Impossible de charger les statistiques des candidatures.');
          this.recruiterStatsLoading.set(false);
        }
      });
    }

    if (role === 'ADMIN') {
      this.auth.users().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: list => this.users.set(list),
        error: () => this.adminError.set('Impossible de charger la liste des utilisateurs.'),
      });
    }
  }
}