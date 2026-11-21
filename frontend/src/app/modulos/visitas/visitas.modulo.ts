import { ModuloFrontend } from '../modulo';

export const visitasModulo: ModuloFrontend = {
  caminho: 'visitas',
  rotas: () => import('./visitas.routes').then((m) => m.VISITAS_ROUTES),
  menu: { rotulo: 'Visitas domiciliares', rotuloCurto: 'Visitas', icone: 'home_health', ordem: 20 },
  permissao: 'VISITAS_LER',
};
