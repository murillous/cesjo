/** Tipos de instituição parceira, com os nomes do enum `TipoParceiro` do backend. */
export type TipoParceiro =
  'ORGAO_PUBLICO' | 'ORGANIZACAO_SOCIEDADE_CIVIL' | 'EMPRESA' | 'INSTITUICAO_ENSINO' | 'OUTRO';

/**
 * Na ordem em que aparecem no formulário.
 *
 * PENDENTE-CESJO(P10): lista provisória de tipos, igual à do backend (ADR 0172).
 */
export const TIPOS_PARCEIRO: readonly { valor: TipoParceiro; rotulo: string }[] = [
  { valor: 'ORGAO_PUBLICO', rotulo: 'Órgão público' },
  { valor: 'ORGANIZACAO_SOCIEDADE_CIVIL', rotulo: 'Organização da sociedade civil' },
  { valor: 'EMPRESA', rotulo: 'Empresa' },
  { valor: 'INSTITUICAO_ENSINO', rotulo: 'Instituição de ensino' },
  { valor: 'OUTRO', rotulo: 'Outro tipo' },
];

export const ROTULOS_TIPO_PARCEIRO: Readonly<Record<TipoParceiro, string>> = Object.fromEntries(
  TIPOS_PARCEIRO.map((t) => [t.valor, t.rotulo]),
) as Record<TipoParceiro, string>;
