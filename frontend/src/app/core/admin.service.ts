import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DemandeRecruteur, StatutDemande } from './models';

const API = 'http://localhost:8081/api';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);

  demandes(statut?: StatutDemande): Observable<DemandeRecruteur[]> {
    let params = new HttpParams();
    if (statut) params = params.set('statut', statut);
    return this.http.get<DemandeRecruteur[]>(`${API}/admin/demandes-recruteur`, { params });
  }

  accepter(id: string): Observable<DemandeRecruteur> {
    return this.http.post<DemandeRecruteur>(`${API}/admin/demandes-recruteur/${id}/accepter`, {});
  }

  refuser(id: string, motif: string): Observable<DemandeRecruteur> {
    return this.http.post<DemandeRecruteur>(`${API}/admin/demandes-recruteur/${id}/refuser`, { motif });
  }
}