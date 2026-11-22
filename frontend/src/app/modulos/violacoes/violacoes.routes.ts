import { Routes } from '@angular/router';

export const VIOLACOES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Violações de direitos | CESJO SGI',
    loadComponent: () => import('./pagina-violacoes').then((m) => m.PaginaViolacoes),
  },
];
