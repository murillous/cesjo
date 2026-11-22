/** Tipos de contato e comunicação do RF011, com os nomes do enum `TipoComunicacao` do backend (ADR 0160). */
export type TipoComunicacao =
  | 'CONTATO_CUIDADOR_FAMILIAR'
  | 'CONTATO_INSTITUCIONAL'
  | 'ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO'
  | 'OFICIO'
  | 'DECLARACAO'
  | 'ENCAMINHAMENTO';

/** Direção dos ofícios, declarações e encaminhamentos, com os nomes do enum `DirecaoComunicacao` (ADR 0164). */
export type DirecaoComunicacao = 'ENVIADA' | 'RECEBIDA';

export interface OpcaoTipo {
  valor: TipoComunicacao;
  rotulo: string;
  /** Ícone Material Symbols que identifica o tipo na lista e no formulário. */
  icone: string;
  /** Rótulo do campo "pessoa ou instituição" para este tipo. */
  contraparte: string;
  /** Rótulos de cada direção, no gênero do tipo. Só nos tipos com direção. */
  direcoes?: Readonly<Record<DirecaoComunicacao, string>>;
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
  {
    valor: 'OFICIO',
    rotulo: 'Ofício',
    icone: 'mail',
    contraparte: 'Instituição',
    direcoes: { ENVIADA: 'Expedido', RECEBIDA: 'Recebido' },
  },
  {
    valor: 'DECLARACAO',
    rotulo: 'Declaração',
    icone: 'verified',
    contraparte: 'Pessoa ou instituição',
    direcoes: { ENVIADA: 'Expedida', RECEBIDA: 'Recebida' },
  },
  {
    valor: 'ENCAMINHAMENTO',
    rotulo: 'Encaminhamento',
    icone: 'forward_to_inbox',
    contraparte: 'Instituição',
    direcoes: { ENVIADA: 'Enviado', RECEBIDA: 'Recebido' },
  },
];

export const TIPO_POR_VALOR: Readonly<Record<TipoComunicacao, OpcaoTipo>> = Object.fromEntries(
  TIPOS_COMUNICACAO.map((t) => [t.valor, t]),
) as Record<TipoComunicacao, OpcaoTipo>;

/** Direções no filtro da lista, que vale para os três tipos com direção. */
export const DIRECOES: readonly { valor: DirecaoComunicacao; rotulo: string }[] = [
  { valor: 'ENVIADA', rotulo: 'Expedidos ou enviados' },
  { valor: 'RECEBIDA', rotulo: 'Recebidos' },
];

/** "Ofício expedido", "Declaração recebida" ou só o rótulo do tipo, quando não tem direção. */
export function rotuloCompleto(tipo: TipoComunicacao, direcao: DirecaoComunicacao | null): string {
  const opcao = TIPO_POR_VALOR[tipo];
  return opcao.direcoes && direcao
    ? `${opcao.rotulo} ${opcao.direcoes[direcao].toLowerCase()}`
    : opcao.rotulo;
}
