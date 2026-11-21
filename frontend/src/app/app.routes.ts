import { Routes } from '@angular/router';

import { exigePermissao } from './acesso/permissao.guard';
import { apenasSemSessao, exigeSessao, exigeSessaoParaTrocarSenha } from './acesso/sessao.guard';
import { MODULOS } from './modulos/registro';

export const routes: Routes = [
  {
    path: 'entrar',
    title: 'Entrar | CESJO SGI',
    canActivate: [apenasSemSessao],
    loadComponent: () => import('./acesso/entrar/pagina-entrar').then((m) => m.PaginaEntrar),
  },
  {
    path: 'trocar-senha',
    title: 'Trocar senha | CESJO SGI',
    canActivate: [exigeSessaoParaTrocarSenha],
    loadComponent: () =>
      import('./acesso/trocar-senha/pagina-trocar-senha').then((m) => m.PaginaTrocarSenha),
  },
  {
    path: '',
    loadComponent: () => import('./layout/layout').then((m) => m.Layout),
    canActivate: [exigeSessao],
    canActivateChild: [exigeSessao],
    children: [
      ...MODULOS.map((modulo) => ({
        path: modulo.caminho,
        loadChildren: modulo.rotas,
        ...(modulo.permissao ? { canMatch: [exigePermissao(modulo.permissao)] } : {}),
      })),
      {
        path: '**',
        title: 'Página não encontrada | CESJO SGI',
        loadComponent: () =>
          import('./paginas/nao-encontrada/pagina-nao-encontrada').then(
            (m) => m.PaginaNaoEncontrada,
          ),
      },
    ],
  },
];
