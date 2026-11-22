import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const ATIVIDADES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Atividades | CESJO SGI',
    loadComponent: () => import('./lista-atividades').then((m) => m.ListaAtividades),
  },
  {
    path: 'nova',
    title: 'Registrar atividade | CESJO SGI',
    canMatch: [exigePermissao('ATIVIDADES_REGISTRAR')],
    loadComponent: () => import('./ficha-atividade').then((m) => m.FichaAtividade),
  },
  {
    path: ':id',
    title: 'Atividade | CESJO SGI',
    loadComponent: () => import('./ficha-atividade').then((m) => m.FichaAtividade),
  },
];
