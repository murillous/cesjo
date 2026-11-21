import { ModuloFrontend } from '../modulo';

export const listaEsperaModulo: ModuloFrontend = {
  caminho: 'lista-espera',
  rotas: () => import('./lista-espera.routes').then((m) => m.LISTA_ESPERA_ROUTES),
  menu: {
    rotulo: 'Lista de espera',
    rotuloCurto: 'Fila',
    icone: 'format_list_numbered',
    ordem: 10,
  },
  permissao: 'LISTA_ESPERA_LER',
};
