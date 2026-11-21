import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const LISTA_ESPERA_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Lista de espera | CESJO SGI',
    loadComponent: () => import('./fila-lista-espera').then((m) => m.FilaListaEspera),
  },
  {
    path: 'nova',
    title: 'Cadastrar candidato | CESJO SGI',
    canMatch: [exigePermissao('LISTA_ESPERA_CADASTRAR')],
    loadComponent: () => import('./formulario-inscricao').then((m) => m.FormularioInscricao),
  },
  {
    path: ':id',
    title: 'Ficha do candidato | CESJO SGI',
    canMatch: [exigePermissao('LISTA_ESPERA_LER')],
    loadComponent: () => import('./ficha-candidato').then((m) => m.FichaCandidato),
  },
  {
    path: ':id/editar',
    title: 'Editar candidato | CESJO SGI',
    canMatch: [exigePermissao('LISTA_ESPERA_EDITAR')],
    loadComponent: () => import('./formulario-inscricao').then((m) => m.FormularioInscricao),
  },
  {
    path: ':id/documentos',
    title: 'Documentos do candidato | CESJO SGI',
    canMatch: [exigePermissao('LISTA_ESPERA_LER')],
    loadComponent: () => import('./documentos-candidato').then((m) => m.DocumentosCandidato),
  },
];
