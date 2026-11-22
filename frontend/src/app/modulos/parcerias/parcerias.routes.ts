import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const PARCERIAS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Parcerias | CESJO SGI',
    loadComponent: () => import('./lista-parceiros').then((m) => m.ListaParceiros),
  },
  {
    path: 'novo',
    title: 'Novo parceiro | CESJO SGI',
    canMatch: [exigePermissao('PARCERIAS_GERENCIAR')],
    loadComponent: () => import('./formulario-parceiro').then((m) => m.FormularioParceiro),
  },
  {
    path: ':id',
    title: 'Parceiro | CESJO SGI',
    loadComponent: () => import('./formulario-parceiro').then((m) => m.FormularioParceiro),
  },
];
