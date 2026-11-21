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
];
