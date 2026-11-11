import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { CANDIDATE_ROUTES, RECRUITER_ROUTES } from './features/offres/offres.routes';
import { roleGuard } from './core/role.guard';

export const routes: Routes = [
  { path: 'connexion', loadComponent: () => import('./features/auth/auth-page').then(m => m.AuthPage), data: { mode: 'login' } },
  { path: 'inscription', loadComponent: () => import('./features/auth/auth-page').then(m => m.AuthPage), data: { mode: 'register' } },
  { path: 'tableau-de-bord', canActivate: [authGuard], loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard) },
  { path: 'mon-cv', canActivate: [authGuard], loadComponent: () => import('./features/condidat/cv-page').then(m => m.CvPage) },
  { path: 'recommandations', canActivate: [authGuard], loadComponent: () => import('./features/condidat/recommendations-page').then(m => m.RecommendationsPage) },
  { path: 'admin/demandes', canActivate: [authGuard, roleGuard(['ADMIN'])], loadComponent: () => import('./features/admin/demandes-page').then(m => m.DemandesPage) },
  { path: 'offres', children: CANDIDATE_ROUTES },
  { path: 'recruteur', children: RECRUITER_ROUTES },
  { path: '', pathMatch: 'full', redirectTo: 'connexion' },
  { path: '**', redirectTo: 'connexion' },
];