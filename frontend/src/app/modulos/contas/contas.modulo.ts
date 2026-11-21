import { ModuloFrontend } from '../modulo';

export const contasModulo: ModuloFrontend = {
  caminho: 'contas',
  rotas: () => import('./contas.routes').then((m) => m.CONTAS_ROUTES),
  menu: { rotulo: 'Contas de acesso', icone: 'manage_accounts', ordem: 90 },
  permissao: 'CONTAS_GERENCIAR',
};
