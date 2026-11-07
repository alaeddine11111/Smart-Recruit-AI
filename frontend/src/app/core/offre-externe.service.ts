import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ExternalOffre } from './offers.models';

@Injectable({ providedIn: 'root' })
export class OffreExterneService {
  private http = inject(HttpClient);

  search(q: string, location: string, page = 1): Observable<ExternalOffre[]> {
    const params = new HttpParams().set('q', q).set('location', location).set('page', page);
    return this.http.get<ExternalOffre[]>(`http://localhost:8084/api/offres-externes`, { params });
  }
}
