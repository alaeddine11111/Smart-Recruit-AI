import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEvent } from '@angular/common/http';
import { Observable, catchError, of, throwError } from 'rxjs';
import { CvInfo } from './models';

const CV_API = 'http://localhost:8082/api/cv';

@Injectable({ providedIn: 'root' })
export class CvService {
  private http = inject(HttpClient);

  /** The candidate's current CV, or null if none yet (the backend answers 404). */
  current(): Observable<CvInfo | null> {
    return this.http.get<CvInfo>(CV_API).pipe(
      catchError((e: HttpErrorResponse) => (e.status === 404 ? of(null) : throwError(() => e))),
    );
  }

  /** Uploads a new CV (replaces the previous one) and reports progress. */
  upload(file: File): Observable<HttpEvent<CvInfo>> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<CvInfo>(CV_API, body, { reportProgress: true, observe: 'events' });
  }
}