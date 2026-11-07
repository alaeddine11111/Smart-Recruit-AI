export type ContractType = 'CDI' | 'CDD' | 'STAGE' | 'FREELANCE' | 'ALTERNANCE';
export type OfferStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';

export const CONTRACT_TYPES: ContractType[] = ['CDI', 'CDD', 'STAGE', 'FREELANCE', 'ALTERNANCE'];
export const OFFER_STATUSES: OfferStatus[] = ['DRAFT', 'PUBLISHED', 'CLOSED'];

export interface Entreprise {
  id?: string;
  nom: string;
  description?: string;
  secteur?: string;
  siteWeb?: string;
  ville?: string;
  adresse?: string;
  telephone?: string;
  emailContact?: string;
  logoUrl?: string;
}

export interface OffreEmploi {
  id: string;
  titre: string;
  description: string;
  localisation: string;
  competences: string[];
  typeContrat: ContractType;
  salaireMin?: number | null;
  salaireMax?: number | null;
  devise?: string;
  statut: OfferStatus;
  entrepriseId: string;
  entrepriseNom: string;
  dateCreation: string;
  datePublication?: string;
}

/** Payload sent to POST/PUT (matches OffreRequest.java) */
export type OffreRequest = Pick<OffreEmploi,
  'titre' | 'description' | 'localisation' | 'competences' | 'typeContrat' | 'statut'> &
  { salaireMin?: number | null; salaireMax?: number | null; devise?: string };

export interface OffreFilter {
  titre?: string;
  localisation?: string;
  competences?: string[];
  typeContrat?: ContractType | '';
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ExternalOffre {
  id: string;
  titre: string;
  entreprise?: string;
  localisation?: string;
  description?: string;
  salaire?: string;
  typeContrat?: string;
  dateMiseAJour?: string;
  urlExterne: string;
  source: string;
}
