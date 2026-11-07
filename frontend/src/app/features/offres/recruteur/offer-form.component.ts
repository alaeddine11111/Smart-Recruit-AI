import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { OffreService } from '../../../core/offre.service';
import { CONTRACT_TYPES, ContractType, OFFER_STATUSES, OfferStatus, OffreRequest } from '../../../core/offers.models';

@Component({
  selector: 'app-offer-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
  <section class="max-w-3xl mx-auto p-6">
    <h1 class="text-2xl font-semibold mb-6">{{ id ? 'Edit offer' : 'Post an offer' }}</h1>

    <form [formGroup]="form" (ngSubmit)="submit()" class="grid gap-4">
      <label class="grid gap-1">Job title *
        <input formControlName="titre" class="border rounded px-3 py-2" />
        @if (form.controls.titre.touched && form.controls.titre.invalid) { <span class="text-red-600 text-sm">Title is required.</span> }
      </label>
      <label class="grid gap-1">Description *
        <textarea formControlName="description" rows="8" class="border rounded px-3 py-2"></textarea>
      </label>
      <div class="grid sm:grid-cols-2 gap-4">
        <label class="grid gap-1">Location *<input formControlName="localisation" placeholder="Tunis, Bizerte, Remote…" class="border rounded px-3 py-2" /></label>
        <label class="grid gap-1">Contract *
          <select formControlName="typeContrat" class="border rounded px-3 py-2">
            @for (t of contracts; track t) { <option [value]="t">{{ t }}</option> }
          </select>
        </label>
        <label class="grid gap-1">Minimum salary<input type="number" formControlName="salaireMin" class="border rounded px-3 py-2" /></label>
        <label class="grid gap-1">Maximum salary<input type="number" formControlName="salaireMax" class="border rounded px-3 py-2" /></label>
      </div>
      <label class="grid gap-1">Required skills * (comma separated)
        <input formControlName="competencesText" placeholder="java, spring boot, mongodb" class="border rounded px-3 py-2" />
      </label>
      <label class="grid gap-1">Status
        <select formControlName="statut" class="border rounded px-3 py-2 w-48">
          @for (s of statuses; track s) { <option [value]="s">{{ s }}</option> }
        </select>
      </label>

      @if (error()) { <p class="text-red-600">{{ error() }}</p> }
      <div class="flex gap-3">
        <button type="submit" [disabled]="form.invalid || saving()" class="bg-blue-600 text-white rounded px-4 py-2 disabled:opacity-50">
          {{ id ? 'Save changes' : 'Create offer' }}
        </button>
        <a routerLink="/recruteur/offres" class="px-4 py-2">Cancel</a>
      </div>
    </form>
  </section>`
})
export class OfferFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private svc = inject(OffreService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  id = this.route.snapshot.paramMap.get('id');
  contracts = CONTRACT_TYPES;
  statuses = OFFER_STATUSES;
  saving = signal(false);
  error = signal('');

  form = this.fb.nonNullable.group({
    titre: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', Validators.required],
    localisation: ['', Validators.required],
    typeContrat: ['CDI' as ContractType, Validators.required],
    salaireMin: [null as number | null],
    salaireMax: [null as number | null],
    competencesText: ['', Validators.required],
    statut: ['DRAFT' as OfferStatus, Validators.required]
  });

  ngOnInit() {
    if (this.id) {
      this.svc.getMine(this.id).subscribe(o =>
        this.form.patchValue({ ...o, competencesText: o.competences.join(', ') }));
    }
  }

  submit() {
    if (this.form.invalid) return;
    const { competencesText, ...v } = this.form.getRawValue();
    const body: OffreRequest = {
      ...v,
      devise: 'TND',
      competences: competencesText.split(',').map(s => s.trim()).filter(Boolean)
    };
    this.saving.set(true);
    const call = this.id ? this.svc.update(this.id, body) : this.svc.create(body);
    call.subscribe({
      next: () => this.router.navigate(['/recruteur/offres']),
      error: (e: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(e.error?.detail ?? 'Could not save the offer.');
      }
    });
  }
}
