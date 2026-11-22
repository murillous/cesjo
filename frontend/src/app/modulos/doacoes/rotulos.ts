/** Direção da doação, com os nomes do enum `DirecaoDoacao` do backend. */
export type DirecaoDoacao = 'RECEBIDA' | 'REALIZADA';

/** O que foi doado, com os nomes do enum `TipoDoacao` do backend. */
export type TipoDoacao =
  | 'DINHEIRO'
  | 'ALIMENTOS'
  | 'VESTUARIO'
  | 'HIGIENE_LIMPEZA'
  | 'MATERIAL_PEDAGOGICO'
  | 'MOVEIS_EQUIPAMENTOS'
  | 'OUTRO';

export const DIRECOES_DOACAO: readonly { valor: DirecaoDoacao; rotulo: string }[] = [
  { valor: 'RECEBIDA', rotulo: 'Recebida pelo Centro-Dia' },
  { valor: 'REALIZADA', rotulo: 'Feita pelo Centro-Dia' },
];

export const ROTULOS_DIRECAO_DOACAO: Readonly<Record<DirecaoDoacao, string>> = {
  RECEBIDA: 'Recebida',
  REALIZADA: 'Realizada',
};

/**
 * Na ordem em que aparecem no formulário.
 *
 * PENDENTE-CESJO(P11): lista provisória de tipos, igual à do backend (ADR 0178).
 */
export const TIPOS_DOACAO: readonly { valor: TipoDoacao; rotulo: string }[] = [
  { valor: 'DINHEIRO', rotulo: 'Dinheiro' },
  { valor: 'ALIMENTOS', rotulo: 'Alimentos' },
  { valor: 'VESTUARIO', rotulo: 'Roupas e calçados' },
  { valor: 'HIGIENE_LIMPEZA', rotulo: 'Higiene e limpeza' },
  { valor: 'MATERIAL_PEDAGOGICO', rotulo: 'Material pedagógico e de oficinas' },
  { valor: 'MOVEIS_EQUIPAMENTOS', rotulo: 'Móveis e equipamentos' },
  { valor: 'OUTRO', rotulo: 'Outro tipo' },
];

export const ROTULOS_TIPO_DOACAO: Readonly<Record<TipoDoacao, string>> = Object.fromEntries(
  TIPOS_DOACAO.map((t) => [t.valor, t.rotulo]),
) as Record<TipoDoacao, string>;
