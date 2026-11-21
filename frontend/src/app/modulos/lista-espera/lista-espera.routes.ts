import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const LISTA_ESPERA_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Lista de espera | CESJO SGI',
    loadComponent: () => import('./pagina-lista-espera').then((m) => m.PaginaListaEspera),
  },
  {
    path: 'nova',
    title: 'Cadastrar candidato | CESJO SGI',
    canMatch: [exigePermissao('LISTA_ESPERA_CADASTRAR')],
    loadComponent: () => import('./formulario-inscricao').then((m) => m.FormularioInscricao),
  },
];
