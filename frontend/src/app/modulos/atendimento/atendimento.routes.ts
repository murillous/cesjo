import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

export const ATENDIMENTO_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Escuta e PIA | CESJO SGI',
    loadComponent: () => import('./lista-pessoas').then((m) => m.ListaPessoas),
  },
  {
    path: 'pessoas/:pessoaId',
    title: 'Atendimento da pessoa | CESJO SGI',
    loadComponent: () => import('./pessoa-atendimento').then((m) => m.PessoaAtendimento),
  },
  {
    path: 'pessoas/:pessoaId/escutas/nova',
    title: 'Registrar escuta | CESJO SGI',
    canMatch: [exigePermissao('ESCUTA_REGISTRAR')],
    loadComponent: () => import('./ficha-escuta').then((m) => m.FichaEscuta),
  },
  {
    path: 'pessoas/:pessoaId/pia/novo',
    title: 'Elaborar PIA | CESJO SGI',
    canMatch: [exigePermissao('PIA_EDITAR')],
    loadComponent: () => import('./ficha-pia').then((m) => m.FichaPia),
  },
  {
    path: 'pias/:id',
    title: 'PIA | CESJO SGI',
    canMatch: [exigePermissao('PIA_LER')],
    loadComponent: () => import('./ficha-pia').then((m) => m.FichaPia),
  },
  {
    path: 'escutas/:id',
    title: 'Escuta | CESJO SGI',
    loadComponent: () => import('./ficha-escuta').then((m) => m.FichaEscuta),
  },
];
