/** Funções dos colaboradores, com os nomes do enum `FuncaoColaborador` do backend. */
export type FuncaoColaborador =
  'ASSISTENTE_SOCIAL' | 'PSICOLOGO' | 'TERAPEUTA_OCUPACIONAL' | 'OUTRA';

/**
 * Na ordem em que aparecem no formulário.
 *
 * PENDENTE-CESJO(P18): lista provisória de funções, igual à do backend (ADR 0045).
 */
export const FUNCOES: readonly { valor: FuncaoColaborador; rotulo: string }[] = [
  { valor: 'ASSISTENTE_SOCIAL', rotulo: 'Assistente social' },
  { valor: 'PSICOLOGO', rotulo: 'Psicólogo(a)' },
  { valor: 'TERAPEUTA_OCUPACIONAL', rotulo: 'Terapeuta ocupacional' },
  { valor: 'OUTRA', rotulo: 'Outra função' },
];

export const ROTULOS_FUNCAO: Readonly<Record<FuncaoColaborador, string>> = Object.fromEntries(
  FUNCOES.map((f) => [f.valor, f.rotulo]),
) as Record<FuncaoColaborador, string>;
