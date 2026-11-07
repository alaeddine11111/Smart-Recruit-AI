import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from './models';
import { AuthService } from './auth.service';

export const roleGuard = (roles: readonly Role[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.user();

  return user && roles.includes(user.role)
    ? true
    : router.createUrlTree(['/connexion']);
};
