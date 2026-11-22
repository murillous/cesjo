import { ModuloFrontend } from '../modulo';

export const atividadesModulo: ModuloFrontend = {
  caminho: 'atividades',
  rotas: () => import('./atividades.routes').then((m) => m.ATIVIDADES_ROUTES),
  menu: { rotulo: 'Atividades', icone: 'groups', ordem: 40 },
  permissao: 'ATIVIDADES_LER',
};
