import { ModuloFrontend } from '../modulo';

export const colaboradoresModulo: ModuloFrontend = {
  caminho: 'colaboradores',
  rotas: () => import('./colaboradores.routes').then((m) => m.COLABORADORES_ROUTES),
  menu: { rotulo: 'Colaboradores', rotuloCurto: 'Equipe', icone: 'badge', ordem: 80 },
  permissao: 'COLABORADORES_LER',
};
