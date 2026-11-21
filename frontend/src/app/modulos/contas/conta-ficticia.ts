import { Conta } from './contas-api';

/**
 * Conta fictícia para os testes do módulo. Fica fora dos arquivos `.spec.ts` porque um spec que
 * importa outro faz o builder de testes perder o `describe` do importado.
 */
export function contaFicticia(mudancas: Partial<Conta> = {}): Conta {
  return {
    id: '00000000-0000-0000-0000-0000000000a1',
    login: 'pessoa.ficticia',
    nome: 'Pessoa Fictícia',
    perfil: 'EQUIPE_TECNICA',
    ativa: true,
    bloqueada: false,
    trocaSenhaObrigatoria: false,
    criadoEm: '2026-11-20T10:00:00Z',
    atualizadoEm: '2026-11-20T10:00:00Z',
    ...mudancas,
  };
}
