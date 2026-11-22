import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const COMUNICACOES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Contatos e comunicações | CESJO SGI',
    loadComponent: () => import('./lista-comunicacoes').then((m) => m.ListaComunicacoes),
  },
  {
    path: 'nova',
    title: 'Registrar contato | CESJO SGI',
    canMatch: [exigePermissao('COMUNICACOES_REGISTRAR')],
    loadComponent: () => import('./ficha-comunicacao').then((m) => m.FichaComunicacao),
  },
  {
    path: ':id',
    title: 'Contato | CESJO SGI',
    loadComponent: () => import('./ficha-comunicacao').then((m) => m.FichaComunicacao),
  },
];
