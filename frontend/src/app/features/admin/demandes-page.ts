import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../core/admin.service';
import { AuthService } from '../../core/auth.service';
import { DemandeRecruteur, StatutDemande } from '../../core/models';

@Component({
  selector: 'app-demandes-page',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './demandes-page.html',
  styleUrl: './demandes-page.css',
})
export class DemandesPage implements OnInit {
  private admin = inject(AdminService);
  readonly auth = inject(AuthService);
  readonly user = this.auth.user;

  readonly onglets: { statut: StatutDemande; label: string }[] = [
    { statut: 'EN_ATTENTE', label: 'En attente' },
    { statut: 'ACCEPTEE', label: 'Acceptées' },
    { statut: 'REFUSEE', label: 'Refusées' },
  ];

  readonly statut = signal<StatutDemande>('EN_ATTENTE');
  readonly demandes = signal<DemandeRecruteur[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly busyId = signal<string | null>(null);
  /** Id of the request whose refusal form is open. */
  readonly refusId = signal<string | null>(null);
  motif = '';

  ngOnInit(): void {
    this.load();
  }

  choisir(s: StatutDemande): void {
    this.statut.set(s);
    this.refusId.set(null);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.admin.demandes(this.statut()).subscribe({
      next: list => {
        this.demandes.set(list);
        this.loading.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(this.messageFrom(e));
      },
    });
  }

  accepter(d: DemandeRecruteur): void {
    this.busyId.set(d.id);
    this.error.set(null);
    this.admin.accepter(d.id).subscribe({
      next: () => this.afterAction(d.id),
      error: (e: HttpErrorResponse) => this.fail(e),
    });
  }

  ouvrirRefus(d: DemandeRecruteur): void {
    this.motif = '';
    this.refusId.set(d.id);
  }

  annulerRefus(): void {
    this.refusId.set(null);
  }

  confirmerRefus(d: DemandeRecruteur): void {
    this.busyId.set(d.id);
    this.error.set(null);
    this.admin.refuser(d.id, this.motif).subscribe({
      next: () => this.afterAction(d.id),
      error: (e: HttpErrorResponse) => this.fail(e),
    });
  }

  private afterAction(id: string): void {
    this.busyId.set(null);
    this.refusId.set(null);
    this.demandes.update(list => list.filter(x => x.id !== id));
  }

  private fail(e: HttpErrorResponse): void {
    this.busyId.set(null);
    this.error.set(this.messageFrom(e));
  }

  private messageFrom(e: HttpErrorResponse): string {
    if (e.status === 0) return 'Impossible de joindre le serveur (auth-service, port 8081).';
    if (e.status === 401 || e.status === 403) return 'Accès réservé à l’administrateur. Reconnectez-vous.';
    return e.error?.message ?? 'Une erreur est survenue. Réessayez.';
  }
}