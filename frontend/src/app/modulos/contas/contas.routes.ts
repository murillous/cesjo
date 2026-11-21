import { Routes } from '@angular/router';

export const CONTAS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Contas de acesso | CESJO SGI',
    loadComponent: () => import('./lista-contas').then((m) => m.ListaContas),
  },
  {
    path: 'nova',
    title: 'Nova conta | CESJO SGI',
    loadComponent: () => import('./formulario-conta').then((m) => m.FormularioConta),
  },
  {
    path: ':id',
    title: 'Conta de acesso | CESJO SGI',
    loadComponent: () => import('./formulario-conta').then((m) => m.FormularioConta),
  },
];
