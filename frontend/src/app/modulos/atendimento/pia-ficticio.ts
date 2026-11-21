import { Pia } from './atendimento-api';

/**
 * PIA fictício para os testes do módulo. Fica fora dos arquivos `.spec.ts` porque um spec que importa
 * outro faz o builder de testes perder o `describe` do importado.
 */
export function piaFicticio(mudancas: Partial<Pia> = {}): Pia {
  return {
    id: 'pia1',
    pessoaId: 'p1',
    nomePessoa: 'Pessoa Fictícia',
    numeroVersao: 1,
    status: 'ELABORADO',
    conteudo: {
      diagnostico: 'Diagnóstico fictício',
      objetivos: [
        {
          descricao: 'Ampliar a autonomia',
          metas: 'Meta fictícia',
          acoes: null,
          prazo: '2027-03-31',
          responsavel: 'Equipe técnica',
        },
      ],
      observacoes: null,
    },
    editavel: true,
    versaoAnteriorId: null,
    criadoEm: '2026-11-21T10:00:00Z',
    atualizadoEm: '2026-11-21T10:00:00Z',
    ...mudancas,
  };
}
