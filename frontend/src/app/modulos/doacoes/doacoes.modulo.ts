import { ModuloFrontend } from '../modulo';

export const doacoesModulo: ModuloFrontend = {
  caminho: 'doacoes',
  rotas: () => import('./doacoes.routes').then((m) => m.DOACOES_ROUTES),
  menu: { rotulo: 'Doações', icone: 'volunteer_activism', ordem: 86 },
  permissao: 'DOACOES_LER',
};
