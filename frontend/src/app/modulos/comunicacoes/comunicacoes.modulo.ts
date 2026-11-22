import { ModuloFrontend } from '../modulo';

export const comunicacoesModulo: ModuloFrontend = {
  caminho: 'comunicacoes',
  rotas: () => import('./comunicacoes.routes').then((m) => m.COMUNICACOES_ROUTES),
  menu: { rotulo: 'Contatos e comunicações', rotuloCurto: 'Contatos', icone: 'call', ordem: 45 },
  permissao: 'COMUNICACOES_LER',
};
