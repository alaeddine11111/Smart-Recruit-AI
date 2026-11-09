import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap, takeWhile, timer } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { CvService } from '../../core/cv.service';
import { ProfilService } from '../../core/profil.service';
import { AnalyseInfo, Competence, CvInfo, Experience, Formation, Profil } from '../../core/models';

const MAX_SIZE = 5 * 1024 * 1024; // 5 Mo

@Component({
  selector: 'app-cv-page',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './cv-page.html',
  styleUrl: './cv-page.css',
})
export class CvPage {
  readonly auth = inject(AuthService);
  private cvService = inject(CvService);
  private profilService = inject(ProfilService);
  private destroyRef = inject(DestroyRef);

  readonly current = signal<CvInfo | null>(null);
  readonly file = signal<File | null>(null);
  readonly dragging = signal(false);
  readonly uploading = signal(false);
  readonly progress = signal(0);
  readonly error = signal<string | null>(null);
  readonly justUploaded = signal(false);
  readonly analyse = signal<AnalyseInfo | null>(null);
  readonly profil = signal<Profil | null>(null);
  readonly profileLoading = signal(true);
  readonly profileSaving = signal(false);
  readonly profileError = signal<string | null>(null);
  readonly profileSuccess = signal<string | null>(null);
  readonly selectedSkillKeys = signal<Set<string>>(new Set());
  readonly skillsSaving = signal(false);
  readonly skillsError = signal<string | null>(null);
  readonly skillsSuccess = signal<string | null>(null);

  personalDraft = { titre: '', resume: '', telephone: '', localisation: '' };
  experienceDraft: Experience = this.emptyExperience();
  formationDraft: Formation = this.emptyFormation();
  readonly experienceFormOpen = signal(false);
  readonly formationFormOpen = signal(false);
  readonly editingExperienceIndex = signal<number | null>(null);
  readonly editingFormationIndex = signal<number | null>(null);

  constructor() {
    this.loadProfile();
    this.cvService.current().subscribe({
      next: cv => {
        this.current.set(cv);
        if (cv) this.suivreAnalyse(); // montre le dernier résultat au rechargement
      },
      error: () => undefined,
    });
  }

  savePersonalInfo(): void {
    this.saveProfileChanges(this.personalDraft, 'Informations personnelles enregistrées.');
  }

  isSkillSelected(skill: Competence): boolean {
    return this.selectedSkillKeys().has(this.skillKey(skill));
  }

  toggleSkill(skill: Competence): void {
    const selected = new Set(this.selectedSkillKeys());
    const key = this.skillKey(skill);
    if (selected.has(key)) selected.delete(key);
    else selected.add(key);
    this.selectedSkillKeys.set(selected);
    this.skillsSuccess.set(null);
    this.skillsError.set(null);
  }

  legacySkills(profil: Profil): Competence[] {
    const known = new Set([
      ...(profil.competencesDetectees ?? []),
      ...(profil.competencesValidees ?? []),
    ].map(skill => this.skillKey(skill)));
    return (profil.competences ?? []).filter(skill => !known.has(this.skillKey(skill)));
  }

  saveSkills(): void {
    const profil = this.profil();
    if (!profil || this.profileSaving()) return;

    const options = this.skillOptions(profil);
    const competencesValidees = options.filter(skill =>
      this.selectedSkillKeys().has(this.skillKey(skill)),
    );

    this.profileSaving.set(true);
    this.skillsSaving.set(true);
    this.skillsError.set(null);
    this.skillsSuccess.set(null);
    this.profilService.save({ ...profil, competencesValidees }).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: saved => {
        this.profil.set(saved);
        this.selectedSkillKeys.set(new Set(competencesValidees.map(skill => this.skillKey(skill))));
        this.profileSaving.set(false);
        this.skillsSaving.set(false);
        this.skillsSuccess.set('Vos compétences validées ont été enregistrées.');
      },
      error: (e: HttpErrorResponse) => {
        this.profileSaving.set(false);
        this.skillsSaving.set(false);
        this.skillsError.set(this.profileMessageFrom(e));
      },
    });
  }

  startExperience(): void {
    this.editingExperienceIndex.set(null);
    this.experienceDraft = this.emptyExperience();
    this.experienceFormOpen.set(true);
  }

  editExperience(index: number): void {
    const experience = this.profil()?.experiences?.[index];
    if (!experience) return;
    this.editingExperienceIndex.set(index);
    this.experienceDraft = { ...experience };
    this.experienceFormOpen.set(true);
  }

  cancelExperience(): void {
    this.editingExperienceIndex.set(null);
    this.experienceDraft = this.emptyExperience();
    this.experienceFormOpen.set(false);
  }

  saveExperience(): void {
    const profil = this.profil();
    if (!profil) return;

    const experiences = [...(profil.experiences ?? [])];
    const index = this.editingExperienceIndex();
    if (index === null) experiences.push({ ...this.experienceDraft });
    else experiences[index] = { ...this.experienceDraft };

    this.saveProfileChanges({ experiences }, 'Expérience enregistrée.', () => this.cancelExperience());
  }

  removeExperience(index: number): void {
    const profil = this.profil();
    if (!profil) return;
    const experiences = (profil.experiences ?? []).filter((_, i) => i !== index);
    this.saveProfileChanges({ experiences }, 'Expérience supprimée.');
  }

  startFormation(): void {
    this.editingFormationIndex.set(null);
    this.formationDraft = this.emptyFormation();
    this.formationFormOpen.set(true);
  }

  editFormation(index: number): void {
    const formation = this.profil()?.formations?.[index];
    if (!formation) return;
    this.editingFormationIndex.set(index);
    this.formationDraft = { ...formation };
    this.formationFormOpen.set(true);
  }

  cancelFormation(): void {
    this.editingFormationIndex.set(null);
    this.formationDraft = this.emptyFormation();
    this.formationFormOpen.set(false);
  }

  saveFormation(): void {
    const profil = this.profil();
    if (!profil) return;

    const formations = [...(profil.formations ?? [])];
    const index = this.editingFormationIndex();
    if (index === null) formations.push({ ...this.formationDraft });
    else formations[index] = { ...this.formationDraft };

    this.saveProfileChanges({ formations }, 'Formation enregistrée.', () => this.cancelFormation());
  }

  removeFormation(index: number): void {
    const profil = this.profil();
    if (!profil) return;
    const formations = (profil.formations ?? []).filter((_, i) => i !== index);
    this.saveProfileChanges({ formations }, 'Formation supprimée.');
  }

  onPick(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.select(input.files?.[0] ?? null);
    input.value = '';
  }

  onDragOver(event: DragEvent): void { event.preventDefault(); this.dragging.set(true); }
  onDragLeave(): void { this.dragging.set(false); }
  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.select(event.dataTransfer?.files?.[0] ?? null);
  }

  clear(): void { this.file.set(null); this.error.set(null); }

  upload(): void {
    const f = this.file();
    if (!f || this.uploading()) return;

    this.uploading.set(true);
    this.progress.set(0);
    this.error.set(null);
    this.analyse.set(null);
    this.profil.set(null);

    this.cvService.upload(f).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.progress.set(Math.round((100 * event.loaded) / event.total));
        } else if (event.type === HttpEventType.Response) {
          this.current.set(event.body);
          this.file.set(null);
          this.uploading.set(false);
          this.justUploaded.set(true);
          this.suivreAnalyse(); // on reste sur la page et on affiche l'extraction
        }
      },
      error: (e: HttpErrorResponse) => {
        this.uploading.set(false);
        this.error.set(this.messageFrom(e));
      },
    });
  }

  size(bytes: number): string {
    return bytes < 1024 * 1024
      ? `${Math.max(1, Math.round(bytes / 1024))} Ko`
      : `${(bytes / 1024 / 1024).toFixed(1)} Mo`;
  }

  /** Interroge l'analyse toutes les 2 s tant qu'elle est en cours, puis charge les données. */
  private suivreAnalyse(): void {
    timer(0, 2000).pipe(
      switchMap(() => this.profilService.analyse()),
      takeWhile(a => a.statut === 'EN_COURS', true),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: a => {
        this.analyse.set(a);
        if (a.statut === 'TERMINEE') this.loadProfile();
      },
      error: () => undefined, // aucune analyse pour l'instant
    });
  }

  private loadProfile(): void {
    this.profileLoading.set(true);
    this.profileError.set(null);
    this.profilService.profil().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: profil => {
        this.profil.set(profil);
        this.selectedSkillKeys.set(new Set(
          (profil.competencesValidees ?? []).map(skill => this.skillKey(skill)),
        ));
        this.personalDraft = {
          titre: profil.titre ?? '',
          resume: profil.resume ?? '',
          telephone: profil.telephone ?? '',
          localisation: profil.localisation ?? '',
        };
        this.profileLoading.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.profileLoading.set(false);
        this.profileError.set(this.profileMessageFrom(e));
      },
    });
  }

  private saveProfileChanges(
    updates: Partial<Profil>,
    successMessage: string,
    onSaved: () => void = () => undefined,
  ): void {
    const profil = this.profil();
    if (!profil || this.profileSaving()) return;

    this.profileSaving.set(true);
    this.profileError.set(null);
    this.profileSuccess.set(null);
    this.profilService.save({ ...profil, ...updates }).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: saved => {
        this.profil.set(saved);
        this.profileSaving.set(false);
        this.profileSuccess.set(successMessage);
        onSaved();
      },
      error: (e: HttpErrorResponse) => {
        this.profileSaving.set(false);
        this.profileError.set(this.profileMessageFrom(e));
      },
    });
  }

  private emptyExperience(): Experience {
    return { poste: '', entreprise: '', dateDebut: '', dateFin: '', description: '' };
  }

  private emptyFormation(): Formation {
    return { diplome: '', etablissement: '', dateDebut: '', dateFin: '' };
  }

  private skillOptions(profil: Profil): Competence[] {
    const options = [
      ...(profil.competencesDetectees ?? []),
      ...(profil.competencesValidees ?? []),
      ...(profil.competences ?? []),
    ];
    const unique = new Map<string, Competence>();
    for (const skill of options) unique.set(this.skillKey(skill), skill);
    return [...unique.values()];
  }

  private skillKey(skill: Competence): string {
    return `${skill.nom.trim().toLocaleLowerCase()}\0${skill.categorie.trim().toLocaleLowerCase()}`;
  }

  private select(f: File | null): void {
    this.error.set(null);
    this.justUploaded.set(false);
    if (!f) return;
    if (f.type !== 'application/pdf') { this.error.set('Le CV doit être un fichier PDF.'); return; }
    if (f.size > MAX_SIZE) { this.error.set('Le fichier dépasse 5 Mo.'); return; }
    this.file.set(f);
  }

  private messageFrom(e: HttpErrorResponse): string {
    if (e.status === 0) return 'Impossible de joindre le service CV (port 8082).';
    if (e.status === 401) return 'Votre session a expiré. Reconnectez-vous.';
    return e.error?.message ?? 'Le dépôt a échoué. Réessayez.';
  }

  private profileMessageFrom(e: HttpErrorResponse): string {
    if (e.status === 0) return 'Impossible de joindre le service profil (port 8082).';
    if (e.status === 401) return 'Votre session a expiré. Reconnectez-vous.';
    return e.error?.message ?? 'Le profil n’a pas pu être enregistré. Réessayez.';
  }
}