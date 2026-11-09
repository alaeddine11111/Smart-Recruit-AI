import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { OffreEmploi, OffreFilter, OffreRequest, OfferStatus, PageResponse } from './offers.models';

@Injectable({ providedIn: 'root' })
export class OffreService {
  private http = inject(HttpClient);
  private api = 'http://localhost:8084/api';

  // ---------- candidate ----------
  search(filter: OffreFilter, page = 0, size = 9): Observable<PageResponse<OffreEmploi>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filter.titre?.trim()) params = params.set('titre', filter.titre.trim());
    if (filter.localisation?.trim()) params = params.set('localisation', filter.localisation.trim());
    if (filter.typeContrat) params = params.set('typeContrat', filter.typeContrat);
    if (filter.competences?.length) params = params.set('competences', filter.competences.join(','));
    return this.http.get<PageResponse<OffreEmploi>>(`${this.api}/offres`, { params });
  }
  getPublished(id: string): Observable<OffreEmploi> { return this.http.get<OffreEmploi>(`${this.api}/offres/${id}`); }

  // ---------- recruiter ----------
  mine(statut?: OfferStatus | '', page = 0, size = 10): Observable<PageResponse<OffreEmploi>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (statut) params = params.set('statut', statut);
    return this.http.get<PageResponse<OffreEmploi>>(`${this.api}/recruteur/offres`, { params });
  }
  getMine(id: string): Observable<OffreEmploi> { return this.http.get<OffreEmploi>(`${this.api}/recruteur/offres/${id}`); }
  create(o: OffreRequest): Observable<OffreEmploi> { return this.http.post<OffreEmploi>(`${this.api}/recruteur/offres`, o); }
  update(id: string, o: OffreRequest): Observable<OffreEmploi> { return this.http.put<OffreEmploi>(`${this.api}/recruteur/offres/${id}`, o); }
  setStatus(id: string, value: OfferStatus): Observable<OffreEmploi> {
    return this.http.patch<OffreEmploi>(`${this.api}/recruteur/offres/${id}/statut`, null, { params: { value } });
  }
  delete(id: string): Observable<void> { return this.http.delete<void>(`${this.api}/recruteur/offres/${id}`); }
}
