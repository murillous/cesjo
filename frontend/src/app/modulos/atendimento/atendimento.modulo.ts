import { ModuloFrontend } from '../modulo';

export const atendimentoModulo: ModuloFrontend = {
  caminho: 'atendimento',
  rotas: () => import('./atendimento.routes').then((m) => m.ATENDIMENTO_ROUTES),
  menu: { rotulo: 'Escuta e PIA', rotuloCurto: 'Escuta', icone: 'psychology', ordem: 30 },
  permissao: 'ESCUTA_LER',
};
