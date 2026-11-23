import { Routes } from '@angular/router';

import { exigePermissao } from '../../acesso/permissao.guard';

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
      {
        path: 'mensal',
        title: 'Relatório mensal | CESJO SGI',
        loadComponent: () => import('./relatorio-mensal').then((m) => m.RelatorioMensal),
      },
      {
        path: 'quadrimestral-e-anual',
        title: 'Relatórios quadrimestral e anual | CESJO SGI',
        loadComponent: () => import('./relatorio-periodo').then((m) => m.RelatorioPeriodo),
      },
      {
        path: 'transparencia',
        title: 'Relatório de transparência | CESJO SGI',
        loadComponent: () =>
          import('./relatorio-transparencia').then((m) => m.RelatorioTransparencia),
      },
      {
        path: 'intervencao',
        title: 'Relatório de intervenção | CESJO SGI',
        // PENDENTE-CESJO(P9): como no backend, pede também VIOLACOES_LER (ADR 0206).
        canMatch: [exigePermissao('VIOLACOES_LER')],
        loadComponent: () =>
          import('./relatorio-intervencao').then((m) => m.RelatorioIntervencao),
      },
    ],
  },
];
