import { ModuloFrontend } from '../modulo';

export const auditoriaModulo: ModuloFrontend = {
  caminho: 'auditoria',
  rotas: () => import('./auditoria.routes').then((m) => m.AUDITORIA_ROUTES),
  menu: { rotulo: 'Log de auditoria', icone: 'manage_search', ordem: 90 },
  permissao: 'AUDITORIA_LER',
};
