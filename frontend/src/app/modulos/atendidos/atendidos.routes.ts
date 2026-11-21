import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const ATENDIDOS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Usuários atendidos | CESJO SGI',
    loadComponent: () => import('./pagina-atendidos').then((m) => m.PaginaAtendidos),
  },
  {
    path: 'inserir/:inscricaoId',
    title: 'Inserir no serviço | CESJO SGI',
    canMatch: [exigePermissao('ATENDIDOS_INSERIR')],
    loadComponent: () => import('./formulario-insercao').then((m) => m.FormularioInsercao),
  },
  {
    path: ':id/desligar',
    title: 'Registrar desligamento | CESJO SGI',
    canMatch: [exigePermissao('ATENDIDOS_DESLIGAR')],
    loadComponent: () => import('./formulario-desligamento').then((m) => m.FormularioDesligamento),
  },
  {
    path: ':id',
    title: 'Ficha do usuário atendido | CESJO SGI',
    loadComponent: () => import('./ficha-atendido').then((m) => m.FichaAtendidoPagina),
  },
];
