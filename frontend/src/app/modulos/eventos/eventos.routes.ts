import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const EVENTOS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Reuniões e eventos | CESJO SGI',
    loadComponent: () => import('./lista-eventos').then((m) => m.ListaEventos),
  },
  {
    path: 'novo',
    title: 'Registrar reunião ou evento | CESJO SGI',
    canMatch: [exigePermissao('EVENTOS_REGISTRAR')],
    loadComponent: () => import('./formulario-evento').then((m) => m.FormularioEvento),
  },
  {
    path: ':id',
    title: 'Reunião ou evento | CESJO SGI',
    loadComponent: () => import('./formulario-evento').then((m) => m.FormularioEvento),
  },
];
