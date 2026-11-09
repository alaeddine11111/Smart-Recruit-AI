import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { OffreService } from '../../../core/offre.service';
import { OFFER_STATUSES, OffreEmploi, OfferStatus } from '../../../core/offers.models';

@Component({
  selector: 'app-offer-list',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe],
  template: `
  <section class="max-w-5xl mx-auto p-6">
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-2xl font-semibold">My job offers</h1>
      <a routerLink="new" class="bg-blue-600 text-white rounded px-4 py-2">Post an offer</a>
    </div>

    <select [(ngModel)]="statut" (ngModelChange)="load(0)" class="border rounded px-3 py-2 mb-4">
      <option value="">All statuses</option>
      @for (s of statuses; track s) { <option [value]="s">{{ s }}</option> }
    </select>

    @if (offers().length === 0) {
      <p class="text-gray-600">No offers here yet. Post your first offer to start receiving candidates.</p>
    } @else {
      <table class="w-full text-left border-collapse">
        <thead><tr class="border-b"><th class="py-2">Title</th><th>Location</th><th>Contract</th><th>Status</th><th>Created</th><th></th></tr></thead>
        <tbody>
        @for (o of offers(); track o.id) {
          <tr class="border-b">
            <td class="py-2 font-medium">{{ o.titre }}</td>
            <td>{{ o.localisation }}</td>
            <td>{{ o.typeContrat }}</td>
            <td>{{ o.statut }}</td>
            <td>{{ o.dateCreation | date:'dd/MM/yyyy' }}</td>
            <td class="text-right whitespace-nowrap space-x-2">
              @if (o.statut !== 'PUBLISHED') { <button (click)="setStatus(o, 'PUBLISHED')" class="text-green-700">Publish</button> }
              @if (o.statut === 'PUBLISHED') { <button (click)="setStatus(o, 'CLOSED')" class="text-amber-700">Close</button> }
              <a [routerLink]="[o.id, 'edit']" class="text-blue-700">Edit</a>
              <button (click)="remove(o)" class="text-red-600">Delete</button>
            </td>
          </tr>
        }
        </tbody>
      </table>
      <div class="flex gap-2 mt-4 items-center">
        <button (click)="load(page() - 1)" [disabled]="page() === 0" class="border rounded px-3 py-1 disabled:opacity-40">Previous</button>
        <span>Page {{ page() + 1 }} / {{ totalPages() || 1 }}</span>
        <button (click)="load(page() + 1)" [disabled]="page() + 1 >= totalPages()" class="border rounded px-3 py-1 disabled:opacity-40">Next</button>
      </div>
    }
  </section>`
})
export class OfferListComponent implements OnInit {
  private svc = inject(OffreService);
  statuses = OFFER_STATUSES;
  statut: OfferStatus | '' = '';
  offers = signal<OffreEmploi[]>([]);
  page = signal(0);
  totalPages = signal(0);

  ngOnInit() { this.load(0); }

  load(p: number) {
    this.svc.mine(this.statut, p).subscribe(r => { this.offers.set(r.content); this.page.set(r.page); this.totalPages.set(r.totalPages); });
  }
  setStatus(o: OffreEmploi, s: OfferStatus) { this.svc.setStatus(o.id, s).subscribe(() => this.load(this.page())); }
  remove(o: OffreEmploi) {
    if (confirm(`Delete "${o.titre}"? This cannot be undone.`)) this.svc.delete(o.id).subscribe(() => this.load(this.page()));
  }
}
