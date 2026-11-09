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
export interface Experience { poste: string; entreprise: string; dateDebut: string; dateFin: string; description: string; }
export interface Formation { diplome: string; etablissement: string; dateDebut: string; dateFin: string; }
export interface Competence { nom: string; categorie: string; }
export interface Langue { nom: string; niveau: string; }
export interface SectionPerso { id: string; titre: string; elements: string[]; }
export interface Profil {
  titre: string; resume: string; telephone: string; localisation: string;
  experiences: Experience[]; formations: Formation[]; competences: Competence[];
  competencesDetectees?: Competence[]; competencesValidees?: Competence[];
  langues: Langue[]; sections: SectionPerso[]; valide: boolean;
}
export interface OffreMatching {
  id: string;
  titre: string;
  competencesRequises: string[];
  source?: string;
  url?: string | null;
}
export interface ResultatMatching {
  offre: OffreMatching;
  score: number;
  competencesCorrespondantes: string[];
  competencesManquantes: string[];
  explication: string;
}
export interface AnalyseInfo {
  statut: 'EN_COURS' | 'TERMINEE' | 'ECHEC';
  messageErreur?: string;
  texteOCR?: string;
  ocrUtilise?: boolean;
}