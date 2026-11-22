import { Parceiro } from './parcerias-api';

/**
 * Parceiro fictício para os testes do módulo. Fica fora dos arquivos `.spec.ts` porque um spec que
 * importa outro faz o builder de testes perder o `describe` do importado.
 */
export function parceiroFicticio(mudancas: Partial<Parceiro> = {}): Parceiro {
  return {
    id: '00000000-0000-0000-0000-0000000000p1',
    nome: 'CRAS Fictício',
    tipo: 'ORGAO_PUBLICO',
    instrumento: null,
    vigenciaInicio: null,
    vigenciaFim: null,
    contato: null,
    observacoes: null,
    ativo: true,
    criadoEm: '2026-11-22T10:00:00Z',
    atualizadoEm: '2026-11-22T10:00:00Z',
    ...mudancas,
  };
}
