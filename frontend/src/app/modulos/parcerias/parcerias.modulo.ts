import { ModuloFrontend } from '../modulo';

export const parceriasModulo: ModuloFrontend = {
  caminho: 'parcerias',
  rotas: () => import('./parcerias.routes').then((m) => m.PARCERIAS_ROUTES),
  menu: { rotulo: 'Parcerias', icone: 'handshake', ordem: 82 },
  permissao: 'PARCERIAS_LER',
};
