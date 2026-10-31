import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';

type Mode = 'login' | 'register';
type RoleChoice = 'CANDIDAT' | 'RECRUTEUR';

interface Part { t: string; hl?: number }   // hl = order of the highlight
interface SheetData {
  key: string;
  name: string;
  sub: string;
  paragraphs: Part[][];
  noteTitle: string;
  tags: string[];
}

const CANDIDATE_SHEET: SheetData = {
  key: 'candidat',
  name: 'AlaEddine Abdelmalek',
  sub: 'Étudiante en développement, ISET Bizerte',
  paragraphs: [
    [
      { t: 'Projet de fin de semestre : application de suivi des stages, avec une API en ' },
      { t: 'Spring Boot', hl: 1 },
      { t: ' et une interface en ' },
      { t: 'Angular', hl: 2 },
      { t: '.' },
    ],
    [
      { t: 'Données stockées dans ' },
      { t: 'MongoDB', hl: 3 },
      { t: ', requêtes testées avec ' },
      { t: 'Postman', hl: 4 },
      { t: '.' },
    ],
    [{ t: 'Licence en technologies de l’information, diplôme prévu en 2027.' }],
  ],
  noteTitle: 'Compétences détectées',
  tags: ['Spring Boot', 'Angular', 'MongoDB', 'Postman'],
};

const OFFER_SHEET: SheetData = {
  key: 'recruteur',
  name: 'Développeur web junior',
  sub: 'Acme Tunisie, Tunis, stage de six mois',
  paragraphs: [
    [
      { t: 'Vous construirez des services ' },
      { t: 'Spring Boot', hl: 1 },
      { t: ' et les écrans ' },
      { t: 'Angular', hl: 2 },
      { t: ' qui les utilisent.' },
    ],
    [
      { t: 'Vous savez interroger une base ' },
      { t: 'MongoDB', hl: 3 },
      { t: ' et vous aimez comprendre un besoin avant de coder.' },
    ],
    [{ t: 'Un vrai projet, un tuteur dédié et une attestation de stage à la fin.' }],
  ],
  noteTitle: 'Compétences demandées',
  tags: ['Spring Boot', 'Angular', 'MongoDB'],
};

@Component({
  selector: 'app-auth-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './auth-page.html',
  styleUrl: './auth-page.css',
})
export class AuthPage {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  readonly mode: Mode = inject(ActivatedRoute).snapshot.data['mode'];
  readonly role = signal<RoleChoice>('CANDIDAT');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly sheet = computed(() => (this.role() === 'CANDIDAT' ? CANDIDATE_SHEET : OFFER_SHEET));

  readonly form = this.fb.nonNullable.group({
    nom: ['', this.mode === 'register' ? [Validators.required] : []],
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', [Validators.required, Validators.minLength(8)]],
    telephone: [''],
    localisation: [''],
    poste: [''],
    entrepriseNom: [''],
  });

  chooseRole(r: RoleChoice): void {
    this.role.set(r);
    const entreprise = this.form.controls.entrepriseNom;
    if (r === 'RECRUTEUR') {
      entreprise.addValidators(Validators.required);
    } else {
      entreprise.removeValidators(Validators.required);
    }
    entreprise.updateValueAndValidity();
  }

  showError(name: 'nom' | 'email' | 'motDePasse' | 'entrepriseNom'): boolean {
    const c = this.form.controls[name];
    return c.invalid && c.touched;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();

    const request$ =
      this.mode === 'login'
        ? this.auth.login({ email: v.email, motDePasse: v.motDePasse })
        : this.role() === 'CANDIDAT'
          ? this.auth.registerCandidat({
              nom: v.nom,
              email: v.email,
              motDePasse: v.motDePasse,
              telephone: v.telephone || undefined,
              localisation: v.localisation || undefined,
            })
          : this.auth.registerRecruteur({
              nom: v.nom,
              email: v.email,
              motDePasse: v.motDePasse,
              poste: v.poste || undefined,
              entrepriseNom: v.entrepriseNom,
            });

    request$.subscribe({
        next: res => this.router.navigate([res.user.role === 'CANDIDAT' ? '/mon-cv' : '/tableau-de-bord']),
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(this.messageFrom(e));
      },
    });
  }

  private messageFrom(e: HttpErrorResponse): string {
    if (e.status === 0) {
      return "Impossible de joindre le serveur. Vérifiez que l'auth-service tourne sur le port 8081.";
    }
    return e.error?.message ?? 'Une erreur est survenue. Réessayez.';
  }
}