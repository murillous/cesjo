import { ModuloFrontend } from '../modulo';

export const atendidosModulo: ModuloFrontend = {
  caminho: 'atendidos',
  rotas: () => import('./atendidos.routes').then((m) => m.ATENDIDOS_ROUTES),
  menu: {
    rotulo: 'Usuários atendidos',
    rotuloCurto: 'Atendidos',
    icone: 'diversity_3',
    ordem: 20,
  },
  permissao: 'ATENDIDOS_LER_BASICO',
};
