import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const DOACOES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Doações | CESJO SGI',
    loadComponent: () => import('./lista-doacoes').then((m) => m.ListaDoacoes),
  },
  {
    path: 'nova',
    title: 'Registrar doação | CESJO SGI',
    canMatch: [exigePermissao('DOACOES_REGISTRAR')],
    loadComponent: () => import('./formulario-doacao').then((m) => m.FormularioDoacao),
  },
  {
    path: ':id',
    title: 'Doação | CESJO SGI',
    loadComponent: () => import('./formulario-doacao').then((m) => m.FormularioDoacao),
  },
];
