import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs';
import { OffreService } from '../../../core/offre.service';
import { CONTRACT_TYPES, OffreEmploi, OffreFilter } from '../../../core/offers.models';

@Component({
  selector: 'app-offer-browse',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  template: `
  <section class="max-w-5xl mx-auto p-6">
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-2xl font-semibold">Job offers</h1>
      <a routerLink="externes" class="text-blue-700">Offers from other sites</a>
    </div>

    <form [formGroup]="filters" class="grid sm:grid-cols-4 gap-3 mb-6">
      <input formControlName="titre" placeholder="Job title" class="border rounded px-3 py-2" />
      <input formControlName="localisation" placeholder="Location" class="border rounded px-3 py-2" />
      <input formControlName="competencesText" placeholder="Skills (java, angular)" class="border rounded px-3 py-2" />
      <select formControlName="typeContrat" class="border rounded px-3 py-2">
        <option value="">All contracts</option>
        @for (t of contracts; track t) { <option [value]="t">{{ t }}</option> }
      </select>
    </form>

    @if (loading()) { <p>Loading offers…</p> }
    @else if (offers().length === 0) { <p class="text-gray-600">No offers match these filters. Try removing one.</p> }

    <div class="grid sm:grid-cols-2 gap-4">
      @for (o of offers(); track o.id) {
        <a [routerLink]="[o.id]" class="border rounded p-4 block hover:bg-gray-50">
          <h2 class="font-semibold">{{ o.titre }}</h2>
          <p class="text-sm text-gray-600">{{ o.entrepriseNom }} · {{ o.localisation }} · {{ o.typeContrat }}</p>
          <div class="flex flex-wrap gap-1 my-2">
            @for (c of o.competences.slice(0, 5); track c) { <span class="bg-gray-100 rounded px-2 py-0.5 text-xs">{{ c }}</span> }
          </div>
          <p class="text-xs text-gray-500">Published {{ o.datePublication | date:'dd/MM/yyyy' }}</p>
        </a>
      }
    </div>

    @if (totalPages() > 1) {
      <div class="flex gap-2 mt-6 items-center">
        <button (click)="go(page() - 1)" [disabled]="page() === 0" class="border rounded px-3 py-1 disabled:opacity-40">Previous</button>
        <span>Page {{ page() + 1 }} / {{ totalPages() }}</span>
        <button (click)="go(page() + 1)" [disabled]="page() + 1 >= totalPages()" class="border rounded px-3 py-1 disabled:opacity-40">Next</button>
      </div>
    }
  </section>`
})
export class OfferBrowseComponent implements OnInit {
  private fb = inject(FormBuilder);
  private svc = inject(OffreService);

  contracts = CONTRACT_TYPES;
  offers = signal<OffreEmploi[]>([]);
  page = signal(0);
  totalPages = signal(0);
  loading = signal(false);

  filters = this.fb.nonNullable.group({ titre: [''], localisation: [''], competencesText: [''], typeContrat: [''] });

  ngOnInit() {
    this.filters.valueChanges.pipe(
      debounceTime(350),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      tap(() => { this.page.set(0); this.loading.set(true); }),
      switchMap(() => this.svc.search(this.currentFilter(), 0))   // switchMap cancels stale requests
    ).subscribe(r => this.apply(r));
    this.go(0);
  }

  go(p: number) {
    this.loading.set(true);
    this.svc.search(this.currentFilter(), p).subscribe(r => this.apply(r));
  }

  private currentFilter(): OffreFilter {
    const v = this.filters.getRawValue();
    return {
      titre: v.titre, localisation: v.localisation, typeContrat: v.typeContrat as OffreFilter['typeContrat'],
      competences: v.competencesText.split(',').map(s => s.trim()).filter(Boolean)
    };
  }
  private apply(r: { content: OffreEmploi[]; page: number; totalPages: number }) {
    this.offers.set(r.content); this.page.set(r.page); this.totalPages.set(r.totalPages); this.loading.set(false);
  }
}
