import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ProfilService } from '../../core/profil.service';
import { Profil, User } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, JsonPipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  readonly auth = inject(AuthService);
  readonly user = this.auth.user;
  private readonly profilService = inject(ProfilService);
  private readonly destroyRef = inject(DestroyRef);

  readonly me = signal<User | null>(null);
  readonly users = signal<User[] | null>(null);
  readonly adminError = signal<string | null>(null);
  readonly candidateProfile = signal<Profil | null>(null);
  readonly candidateLoading = signal(false);
  readonly candidateError = signal<string | null>(null);

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

    if (role === 'ADMIN') {
      this.auth.users().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: list => this.users.set(list),
        error: () => this.adminError.set('Impossible de charger la liste des utilisateurs.'),
      });
    }
  }
}