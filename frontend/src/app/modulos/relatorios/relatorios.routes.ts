import { Routes } from '@angular/router';

export const RELATORIOS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pagina-relatorios').then((m) => m.PaginaRelatorios),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'perfil' },
      {
        path: 'perfil',
        title: 'Perfil dos usuários atendidos | CESJO SGI',
        loadComponent: () => import('./painel-perfil').then((m) => m.PainelPerfil),
      },
      {
        path: 'atendimento',
        title: 'Violações, atividades e desligamentos | CESJO SGI',
        loadComponent: () => import('./painel-atendimento').then((m) => m.PainelAtendimento),
      },
    ],
  },
];
