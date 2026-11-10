import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ExternalOffre, OffreEmploi } from './offers.models';
import { OffreMatching, ResultatMatching } from './models';

const API = 'http://localhost:8082/api';

const KNOWN_SKILLS = [
  'Spring Boot', 'Machine Learning', 'Power BI', 'JavaScript', 'TypeScript',
  'PostgreSQL', 'TensorFlow', 'Kubernetes', 'Bootstrap', 'Tailwind', 'Node.js',
  'Express', 'Hibernate', 'Symfony', 'Laravel', 'FastAPI', 'Java', 'Spring',
  'Python', 'Angular', 'React', 'Vue', 'PHP', 'SQL', 'MySQL', 'MongoDB',
  'Oracle', 'Docker', 'GitHub', 'GitLab', 'Jenkins', 'Linux', 'Azure', 'AWS',
  'Maven', 'REST', 'HTML', 'CSS', 'Flutter', 'Android', 'Kotlin', 'Swift',
  'Figma', 'Scrum', 'Agile', 'Jira', 'Excel', 'Django', 'Flask', 'Firebase',
  'Selenium', 'Postman', 'MATLAB', 'Arduino', 'C++', 'C#', '.NET', 'JPA', 'Git',
];

@Injectable({ providedIn: 'root' })
export class MatchingService {
  private http = inject(HttpClient);

  evaluer(offres: OffreMatching[]): Observable<ResultatMatching[]> {
    return this.http.post<ResultatMatching[]>(`${API}/matching`, offres);
  }

  internalOffer(offer: OffreEmploi): OffreMatching {
    return {
      id: offer.id,
      titre: offer.titre,
      competencesRequises: offer.competences ?? [],
      source: 'INTERNE',
      url: `/offres/${encodeURIComponent(offer.id)}`,
      entreprise: offer.entrepriseNom,
      localisation: offer.localisation,
      description: offer.description,
    };
  }

  externalOffer(offer: ExternalOffre): OffreMatching | null {
    const text = this.normalize(`${offer.titre} ${offer.description ?? ''}`);
    const competencesRequises = KNOWN_SKILLS.filter(skill => {
      const normalizedSkill = this.normalize(skill).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      return new RegExp(`(^|[^a-z0-9])${normalizedSkill}(?=$|[^a-z0-9])`).test(text);
    });

    if (competencesRequises.length === 0) return null;

    return {
      id: `external:${offer.id}`,
      titre: offer.titre,
      competencesRequises,
      source: offer.source,
      url: offer.urlExterne,
      entreprise: offer.entreprise,
      localisation: offer.localisation,
      description: offer.description,
    };
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLocaleLowerCase();
  }
}
