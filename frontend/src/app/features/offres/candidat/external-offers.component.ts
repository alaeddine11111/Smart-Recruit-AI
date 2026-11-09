import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { OffreExterneService } from '../../../core/offre-externe.service';
import { ExternalOffre } from '../../../core/offers.models';

@Component({
  selector: 'app-external-offers',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
  <section class="max-w-5xl mx-auto p-6">
    <a routerLink="/offres" class="text-blue-700">Back to Smart-Recruit offers</a>
    <h1 class="text-2xl font-semibold my-4">Offers from other sites</h1>
    <p class="text-sm text-gray-600 mb-4">These offers are published by third parties. You apply on their website.</p>

    <form [formGroup]="form" (ngSubmit)="search()" class="flex flex-wrap gap-3 mb-6">
      <input formControlName="q" placeholder="Keywords (e.g. développeur java)" class="border rounded px-3 py-2 flex-1 min-w-48" />
      <input formControlName="location" placeholder="Location (optional)" class="border rounded px-3 py-2" />
      <button class="bg-blue-600 text-white rounded px-4 py-2">Search</button>
    </form>

    @if (loading()) { <p>Searching…</p> }
    @else if (searched() && results().length === 0) { <p class="text-gray-600">No external offers found. Try broader keywords.</p> }

    <div class="grid gap-4">
      @for (o of results(); track o.id) {
        <article class="border rounded p-4">
          <h2 class="font-semibold">{{ o.titre }}</h2>
          <p class="text-sm text-gray-600">{{ o.entreprise }} · {{ o.localisation }} · via {{ o.source }}</p>
          <p class="text-sm mt-2 line-clamp-3">{{ o.description }}</p>
          <a [href]="o.urlExterne" target="_blank" rel="noopener noreferrer"
             class="inline-block mt-3 border border-blue-600 text-blue-700 rounded px-3 py-1">Apply on {{ o.source }}</a>
        </article>
      }
    </div>
  </section>`
})
export class ExternalOffersComponent {
  private fb = inject(FormBuilder);
  private svc = inject(OffreExterneService);

  form = this.fb.nonNullable.group({ q: [''], location: [''] });
  results = signal<ExternalOffre[]>([]);
  loading = signal(false);
  searched = signal(false);

  search() {
    const { q, location } = this.form.getRawValue();
    this.loading.set(true);
    this.svc.search(q, location).subscribe({
      next: r => { this.results.set(r); this.loading.set(false); this.searched.set(true); },
      error: () => { this.results.set([]); this.loading.set(false); this.searched.set(true); }
    });
  }
}
