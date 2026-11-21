import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const COLABORADORES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Colaboradores | CESJO SGI',
    loadComponent: () => import('./lista-colaboradores').then((m) => m.ListaColaboradores),
  },
  {
    path: 'novo',
    title: 'Novo colaborador | CESJO SGI',
    canMatch: [exigePermissao('COLABORADORES_GERENCIAR')],
    loadComponent: () => import('./formulario-colaborador').then((m) => m.FormularioColaborador),
  },
  {
    path: ':id',
    title: 'Colaborador | CESJO SGI',
    loadComponent: () => import('./formulario-colaborador').then((m) => m.FormularioColaborador),
  },
];
