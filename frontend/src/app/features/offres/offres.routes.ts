import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth.guard';
import { roleGuard } from '../../core/role.guard';

export const RECRUITER_ROUTES: Routes = [
  { path: 'entreprise', canActivate: [roleGuard(['RECRUTEUR', 'ADMIN'])], loadComponent: () => import('./recruteur/company-profile.component').then(m => m.CompanyProfileComponent) },
  { path: 'offres', canActivate: [roleGuard(['RECRUTEUR', 'ADMIN'])], loadComponent: () => import('./recruteur/offer-list.component').then(m => m.OfferListComponent) },
  { path: 'offres/new', canActivate: [roleGuard(['RECRUTEUR', 'ADMIN'])], loadComponent: () => import('./recruteur/offer-form.component').then(m => m.OfferFormComponent) },
  { path: 'offres/:id/edit', canActivate: [roleGuard(['RECRUTEUR', 'ADMIN'])], loadComponent: () => import('./recruteur/offer-form.component').then(m => m.OfferFormComponent) },
];

// 'externes' must stay before ':id'
export const CANDIDATE_ROUTES: Routes = [
  { path: '', canActivate: [authGuard], loadComponent: () => import('./candidat/offer-browse.component').then(m => m.OfferBrowseComponent) },
  { path: 'externes', canActivate: [authGuard], loadComponent: () => import('./candidat/external-offers.component').then(m => m.ExternalOffersComponent) },
  { path: ':id', canActivate: [authGuard], loadComponent: () => import('./candidat/offer-detail.component').then(m => m.OfferDetailComponent) },
];
