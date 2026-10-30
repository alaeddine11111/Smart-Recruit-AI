import { Component, inject, signal } from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { AuthService } from '../../core/auth.service';
import { User } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, JsonPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  readonly auth = inject(AuthService);
  readonly user = this.auth.user;

  readonly me = signal<User | null>(null);
  readonly users = signal<User[] | null>(null);
  readonly adminError = signal<string | null>(null);

  constructor() {
    // Proves the token is sent and accepted by the backend
    this.auth.me().subscribe({ next: u => this.me.set(u) });

    if (this.user()?.role === 'ADMIN') {
      this.auth.users().subscribe({
        next: list => this.users.set(list),
        error: () => this.adminError.set('Impossible de charger la liste des utilisateurs.'),
      });
    }
  }
}