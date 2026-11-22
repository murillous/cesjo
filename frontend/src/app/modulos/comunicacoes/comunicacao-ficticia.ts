import { Comunicacao } from './comunicacoes-api';

/** Contato fictício para os testes das telas. */
export function comunicacaoFicticia(mudancas: Partial<Comunicacao> = {}): Comunicacao {
  return {
    id: 'c1',
    tipo: 'CONTATO_INSTITUCIONAL',
    data: '2026-11-20',
    contraparte: 'CRAS Fictício',
    assunto: 'Vaga para usuário',
    descricao: 'Retorno combinado (fictício).',
    criadoEm: '2026-11-20T13:00:00Z',
    atualizadoEm: '2026-11-20T13:00:00Z',
    ...mudancas,
  };
}
