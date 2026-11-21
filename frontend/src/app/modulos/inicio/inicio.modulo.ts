import { ModuloFrontend } from '../modulo';

export const inicioModulo: ModuloFrontend = {
  caminho: '',
  rotas: () => import('./inicio.routes').then((m) => m.INICIO_ROUTES),
  menu: { rotulo: 'Início', icone: 'pi pi-home', ordem: 0 },
};
