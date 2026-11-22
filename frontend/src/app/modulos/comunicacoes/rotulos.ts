/** Tipos de contato e comunicação do RF011, com os nomes do enum `TipoComunicacao` do backend (ADR 0160). */
export type TipoComunicacao =
  'CONTATO_CUIDADOR_FAMILIAR' | 'CONTATO_INSTITUCIONAL' | 'ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO';

export interface OpcaoTipo {
  valor: TipoComunicacao;
  rotulo: string;
  /** Ícone Material Symbols que identifica o tipo na lista e no formulário. */
  icone: string;
  /** Rótulo do campo "pessoa ou instituição" para este tipo. */
  contraparte: string;
}

/** Tipos na ordem do documento de requisitos. */
export const TIPOS_COMUNICACAO: readonly OpcaoTipo[] = [
  {
    valor: 'CONTATO_CUIDADOR_FAMILIAR',
    rotulo: 'Contato telefônico com cuidador familiar',
    icone: 'family_restroom',
    contraparte: 'Cuidador familiar',
  },
  {
    valor: 'CONTATO_INSTITUCIONAL',
    rotulo: 'Contato telefônico institucional',
    icone: 'apartment',
    contraparte: 'Instituição',
  },
  {
    valor: 'ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO',
    rotulo: 'Atendimento a público não acompanhado',
    icone: 'support_agent',
    contraparte: 'Pessoa atendida',
  },
];

export const TIPO_POR_VALOR: Readonly<Record<TipoComunicacao, OpcaoTipo>> = Object.fromEntries(
  TIPOS_COMUNICACAO.map((t) => [t.valor, t]),
) as Record<TipoComunicacao, OpcaoTipo>;
