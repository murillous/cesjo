import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const VISITAS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Visitas domiciliares | CESJO SGI',
    loadComponent: () => import('./agenda-visitas').then((m) => m.AgendaVisitas),
  },
  {
    path: 'nova',
    title: 'Agendar visita | CESJO SGI',
    canMatch: [exigePermissao('VISITAS_AGENDAR')],
    loadComponent: () => import('./formulario-visita').then((m) => m.FormularioVisita),
  },
  {
    path: ':id',
    title: 'Visita domiciliar | CESJO SGI',
    loadComponent: () => import('./ficha-visita').then((m) => m.FichaVisita),
  },
];
