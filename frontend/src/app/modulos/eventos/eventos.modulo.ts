import { ModuloFrontend } from '../modulo';

export const eventosModulo: ModuloFrontend = {
  caminho: 'eventos',
  rotas: () => import('./eventos.routes').then((m) => m.EVENTOS_ROUTES),
  menu: { rotulo: 'Reuniões e eventos', rotuloCurto: 'Eventos', icone: 'event', ordem: 84 },
  permissao: 'EVENTOS_LER',
};
