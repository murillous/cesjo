import { Conta } from './contas-api';

/** Situação da conta em palavras, na ordem de importância para quem administra. */
export function situacaoConta(conta: Conta): string {
  if (!conta.ativa) {
    return 'Desativada';
  }
  if (conta.bloqueada) {
    return 'Bloqueada por tentativas erradas';
  }
  if (conta.trocaSenhaObrigatoria) {
    return 'Aguardando troca de senha';
  }
  return 'Ativa';
}
