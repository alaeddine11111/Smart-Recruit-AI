import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { EntrepriseService } from '../../../core/entreprise.service';

@Component({
  selector: 'app-company-profile',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
  <section class="max-w-3xl mx-auto p-6">
    <h1 class="text-2xl font-semibold mb-1">Company profile</h1>
    <p class="text-gray-600 mb-6">
      {{ exists() ? 'Candidates see this on your job offers.' : 'Create your company profile to start posting offers.' }}
    </p>

    <form [formGroup]="form" (ngSubmit)="save()" class="grid gap-4">
      <label class="grid gap-1">Company name *
        <input formControlName="nom" class="border rounded px-3 py-2" />
        @if (form.controls.nom.touched && form.controls.nom.invalid) { <span class="text-red-600 text-sm">Name is required.</span> }
      </label>
      <label class="grid gap-1">Description
        <textarea formControlName="description" rows="4" class="border rounded px-3 py-2"></textarea>
      </label>
      <div class="grid sm:grid-cols-2 gap-4">
        <label class="grid gap-1">Sector <input formControlName="secteur" class="border rounded px-3 py-2" /></label>
        <label class="grid gap-1">City <input formControlName="ville" class="border rounded px-3 py-2" /></label>
        <label class="grid gap-1">Website <input formControlName="siteWeb" class="border rounded px-3 py-2" /></label>
        <label class="grid gap-1">Contact email <input formControlName="emailContact" type="email" class="border rounded px-3 py-2" /></label>
        <label class="grid gap-1">Phone <input formControlName="telephone" class="border rounded px-3 py-2" /></label>
        <label class="grid gap-1">Logo URL <input formControlName="logoUrl" class="border rounded px-3 py-2" /></label>
      </div>
      <label class="grid gap-1">Address <input formControlName="adresse" class="border rounded px-3 py-2" /></label>

      <div class="flex items-center gap-3">
        <button type="submit" [disabled]="form.invalid || saving()" class="bg-blue-600 text-white rounded px-4 py-2 disabled:opacity-50">
          {{ saving() ? 'Saving…' : 'Save company' }}
        </button>
        @if (message()) { <span class="text-sm" [class.text-green-700]="!isError()" [class.text-red-600]="isError()">{{ message() }}</span> }
      </div>
    </form>
  </section>`
})
export class CompanyProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private svc = inject(EntrepriseService);

  exists = signal(false);
  saving = signal(false);
  message = signal('');
  isError = signal(false);

  form = this.fb.nonNullable.group({
    nom: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''], secteur: [''], ville: [''], siteWeb: [''],
    emailContact: ['', Validators.email], telephone: [''], logoUrl: [''], adresse: ['']
  });

  ngOnInit() {
    this.svc.getMine().subscribe({
      next: e => { this.exists.set(true); this.form.patchValue(e); },
      error: (err: HttpErrorResponse) => { if (err.status !== 404) this.fail('Could not load your company.'); }
    });
  }

  save() {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.svc.saveMine(this.form.getRawValue()).subscribe({
      next: () => { this.exists.set(true); this.saving.set(false); this.ok('Company saved.'); },
      error: () => { this.saving.set(false); this.fail('Could not save. Check the fields and try again.'); }
    });
  }
  private ok(m: string) { this.isError.set(false); this.message.set(m); }
  private fail(m: string) { this.isError.set(true); this.message.set(m); }
}
