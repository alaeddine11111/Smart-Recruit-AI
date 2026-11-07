import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OffreService } from '../../../core/offre.service';
import { EntrepriseService } from '../../../core/entreprise.service';
import { Entreprise, OffreEmploi } from '../../../core/offers.models';

@Component({
  selector: 'app-offer-detail',
  standalone: true,
  imports: [RouterLink, DatePipe],
  template: `
  <section class="max-w-3xl mx-auto p-6">
    <a routerLink="/offres" class="text-blue-700">Back to offers</a>
    @if (notFound()) {
      <p class="mt-6">This offer is no longer available.</p>
    } @else if (offer(); as o) {
      <h1 class="text-2xl font-semibold mt-4">{{ o.titre }}</h1>
      <p class="text-gray-600">{{ o.entrepriseNom }} · {{ o.localisation }} · {{ o.typeContrat }}</p>
      @if (o.salaireMin || o.salaireMax) {
        <p class="mt-1">Salary: {{ o.salaireMin ?? '?' }} – {{ o.salaireMax ?? '?' }} {{ o.devise }}</p>
      }
      <div class="flex flex-wrap gap-1 my-3">
        @for (c of o.competences; track c) { <span class="bg-gray-100 rounded px-2 py-0.5 text-sm">{{ c }}</span> }
      </div>
      <p class="whitespace-pre-line">{{ o.description }}</p>
      <p class="text-sm text-gray-500 mt-4">Published {{ o.datePublication | date:'dd/MM/yyyy' }}</p>

      <!-- Hook for the next module (applications): [routerLink]="['/candidature', o.id]" -->
      <button class="bg-blue-600 text-white rounded px-4 py-2 mt-6" disabled title="Applications module coming next">Apply</button>

      @if (company(); as c) {
        <div class="border rounded p-4 mt-8">
          <h2 class="font-semibold">About {{ c.nom }}</h2>
          <p class="text-sm text-gray-600">{{ c.secteur }} {{ c.ville ? '· ' + c.ville : '' }}</p>
          <p class="mt-2">{{ c.description }}</p>
        </div>
      }
    }
  </section>`
})
export class OfferDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private offres = inject(OffreService);
  private entreprises = inject(EntrepriseService);

  offer = signal<OffreEmploi | null>(null);
  company = signal<Entreprise | null>(null);
  notFound = signal(false);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.offres.getPublished(id).subscribe({
      next: o => { this.offer.set(o); this.entreprises.getById(o.entrepriseId).subscribe(c => this.company.set(c)); },
      error: () => this.notFound.set(true)
    });
  }
}
