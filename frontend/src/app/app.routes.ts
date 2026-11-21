import { Routes } from '@angular/router';

import { exigePermissao } from './acesso/permissao.guard';
import { Layout } from './layout/layout';
import { MODULOS } from './modulos/registro';

export const routes: Routes = [
  {
    path: '',
    component: Layout,
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
