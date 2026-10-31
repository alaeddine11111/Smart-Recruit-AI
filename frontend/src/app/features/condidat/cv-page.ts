import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap, takeWhile, timer } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { CvService } from '../../core/cv.service';
import { ProfilService } from '../../core/profil.service';
import { AnalyseInfo, CvInfo, Profil } from '../../core/models';

const MAX_SIZE = 5 * 1024 * 1024; // 5 Mo

@Component({
  selector: 'app-cv-page',
  imports: [DatePipe],
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

  constructor() {
    this.cvService.current().subscribe({
      next: cv => {
        this.current.set(cv);
        if (cv) this.suivreAnalyse(); // montre le dernier résultat au rechargement
      },
      error: () => undefined,
    });
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
        if (a.statut === 'TERMINEE') {
          this.profilService.profil().subscribe(p => this.profil.set(p));
        }
      },
      error: () => undefined, // aucune analyse pour l'instant
    });
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
}