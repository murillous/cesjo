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
  // A fila é de todos os perfis; o Consulta vê só os dados básicos (ADR 0058).
  permissao: 'LISTA_ESPERA_FILA_LER',
};
