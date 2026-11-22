import { ModuloFrontend } from '../modulo';

/** Dashboards (RF017) e relatórios estatísticos (RF016), do módulo `estatisticas` do backend. */
export const relatoriosModulo: ModuloFrontend = {
  caminho: 'relatorios',
  rotas: () => import('./relatorios.routes').then((m) => m.RELATORIOS_ROUTES),
  menu: { rotulo: 'Relatórios', icone: 'bar_chart', ordem: 70 },
  // PENDENTE-CESJO(P17): só Coordenador e Consulta veem relatórios (ADR 0196).
  permissao: 'RELATORIOS_LER',
};
