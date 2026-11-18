import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth.guard';
import { roleGuard } from '../../core/role.guard';

export const RECRUITER_ROUTES: Routes = [
  { path: 'entreprise', canActivate: [roleGuard(['RECRUTEUR', 'ADMIN'])], loadComponent: () => import('./recruteur/company-profile.component').then(m => m.CompanyProfileComponent) },
  { path: 'offres', canActivate: [roleGuard(['RECRUTEUR', 'ADMIN'])], loadComponent: () => import('./recruteur/offer-list.component').then(m => m.OfferListComponent) },
  { path: 'offres/new', canActivate: [roleGuard(['RECRUTEUR', 'ADMIN'])], loadComponent: () => import('./recruteur/offer-form.component').then(m => m.OfferFormComponent) },
  { path: 'offres/:id/edit', canActivate: [roleGuard(['RECRUTEUR', 'ADMIN'])], loadComponent: () => import('./recruteur/offer-form.component').then(m => m.OfferFormComponent) },
  { path: 'candidatures', canActivate: [roleGuard(['RECRUTEUR', 'ADMIN'])], loadComponent: () => import('./recruteur/applications-list.component').then(m => m.ApplicationsListComponent) },
  { path: 'candidatures/:id', canActivate: [roleGuard(['RECRUTEUR', 'ADMIN'])], loadComponent: () => import('./recruteur/application-detail.component').then(m => m.ApplicationDetailComponent) },
];

// 'externes' must stay before ':id'
export const CANDIDATE_ROUTES: Routes = [
  { path: '', canActivate: [authGuard], loadComponent: () => import('./candidat/offer-browse.component').then(m => m.OfferBrowseComponent) },
  { path: 'externes', loadComponent: () => import('./candidat/external-offers.component').then(m => m.ExternalOffersComponent) },
  { path: ':id', canActivate: [authGuard], loadComponent: () => import('./candidat/offer-detail.component').then(m => m.OfferDetailComponent) },
];
