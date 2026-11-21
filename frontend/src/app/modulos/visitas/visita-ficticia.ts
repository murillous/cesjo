import { Visita } from './visitas-api';

/**
 * Visita fictícia para os testes do módulo. Fica fora dos arquivos `.spec.ts` porque um spec que
 * importa outro faz o builder de testes perder o `describe` do importado.
 */
export function visitaFicticia(mudancas: Partial<Visita> = {}): Visita {
  return {
    id: '00000000-0000-0000-0000-0000000000f1',
    pessoaId: '00000000-0000-0000-0000-0000000000e1',
    nomePessoa: 'Candidata Fictícia',
    dataHora: '2026-11-25T14:30:00',
    status: 'AGENDADA',
    profissionais: [{ id: 'c1', nome: 'Colaboradora Fictícia', funcao: 'ASSISTENTE_SOCIAL' }],
    criadoEm: '2026-11-21T10:00:00Z',
    ...mudancas,
  };
}
