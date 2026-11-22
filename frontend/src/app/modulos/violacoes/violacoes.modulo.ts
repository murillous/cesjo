import { ModuloFrontend } from '../modulo';

/** Lista geral das violações de direitos (RF009, ADR 0145). As telas de registro ficam na ficha. */
export const violacoesModulo: ModuloFrontend = {
  caminho: 'violacoes',
  rotas: () => import('./violacoes.routes').then((m) => m.VIOLACOES_ROUTES),
  menu: {
    rotulo: 'Violações de direitos',
    rotuloCurto: 'Violações',
    icone: 'shield_person',
    ordem: 25,
  },
  permissao: 'VIOLACOES_LER',
};
