import { Colaborador } from './colaboradores-api';

/**
 * Colaborador fictício para os testes do módulo. Fica fora dos arquivos `.spec.ts` porque um spec
 * que importa outro faz o builder de testes perder o `describe` do importado.
 */
export function colaboradorFicticio(mudancas: Partial<Colaborador> = {}): Colaborador {
  return {
    id: '00000000-0000-0000-0000-0000000000c1',
    nome: 'Colaboradora Fictícia',
    funcao: 'ASSISTENTE_SOCIAL',
    ativo: true,
    conta: null,
    criadoEm: '2026-11-21T10:00:00Z',
    atualizadoEm: '2026-11-21T10:00:00Z',
    ...mudancas,
  };
}
