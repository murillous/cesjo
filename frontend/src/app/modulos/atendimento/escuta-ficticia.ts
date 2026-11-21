import { Escuta } from './atendimento-api';

/**
 * Escuta fictícia para os testes do módulo. Fica fora dos arquivos `.spec.ts` porque um spec que
 * importa outro faz o builder de testes perder o `describe` do importado.
 */
export function escutaFicticia(mudancas: Partial<Escuta> = {}): Escuta {
  return {
    id: 'e1',
    pessoaId: 'p1',
    nomePessoa: 'Pessoa Fictícia',
    dataEscuta: '2026-11-20',
    profissional: { id: 'c1', nome: 'Psicóloga Fictícia', funcao: 'PSICOLOGO' },
    dinamicaFamiliar: 'Texto fictício de dinâmica familiar.',
    necessidadesImediatas: null,
    habilidades: null,
    potencialidades: null,
    criadoEm: '2026-11-20T10:00:00Z',
    atualizadoEm: '2026-11-20T10:00:00Z',
    ...mudancas,
  };
}
