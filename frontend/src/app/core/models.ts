export type Role = 'CANDIDAT' | 'RECRUTEUR' | 'ADMIN';

export interface User {
  id: string;
  nom: string;
  email: string;
  role: Role;
  dateCreation: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  motDePasse: string;
}

export interface RegisterCandidatRequest {
  nom: string;
  email: string;
  motDePasse: string;
  telephone?: string;
  localisation?: string;
}

export interface RegisterRecruteurRequest {
  nom: string;
  email: string;
  motDePasse: string;
  poste?: string;
  entrepriseNom: string;
  entrepriseDescription?: string;
  entrepriseSiteWeb?: string;
}
export interface CvInfo {
  id: string;
  nomFichier: string;
  taille: number;
  dateDepot: string;
}