import { Doacao } from './doacoes-api';

/**
 * Doação fictícia para os testes do módulo. Fica fora dos arquivos `.spec.ts` porque um spec que
 * importa outro faz o builder de testes perder o `describe` do importado.
 */
export function doacaoFicticia(mudancas: Partial<Doacao> = {}): Doacao {
  return {
    id: '00000000-0000-0000-0000-0000000000d1',
    direcao: 'RECEBIDA',
    data: '2026-05-04',
    contraparte: 'Mercado Fictício',
    tipo: 'ALIMENTOS',
    descricao: 'Cestas básicas.',
    quantidade: '5 cestas',
    valor: null,
    criadoEm: '2026-05-04T12:00:00Z',
    atualizadoEm: '2026-05-04T12:00:00Z',
    ...mudancas,
  };
}
