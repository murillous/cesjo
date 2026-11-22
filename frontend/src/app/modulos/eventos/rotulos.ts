/** Os sete tipos do RF012, com os nomes do enum `TipoEventoInstitucional` do backend. */
export type TipoEvento =
  | 'REUNIAO_COORDENACAO_EQUIPE'
  | 'FORMACAO_MULTIPROFISSIONAL'
  | 'REUNIAO_DIRECAO_GERAL'
  | 'MONITORAMENTO_ORGAO_GESTOR'
  | 'VISITA_INSTITUCIONAL'
  | 'EVENTO'
  | 'REUNIAO_PARCEIROS';

/** Indicação do RF012 para visitas e eventos. */
export type DirecaoEvento = 'REALIZADO' | 'RECEBIDO' | 'PARTICIPADO';

interface OpcaoDirecao {
  valor: DirecaoEvento;
  rotulo: string;
}

/**
 * Tipos na ordem do RF012, com as indicações que cada um aceita (iguais às do backend, ADR 0175).
 * Sem `direcoes`, o tipo não tem indicação.
 */
export const TIPOS_EVENTO: readonly {
  valor: TipoEvento;
  rotulo: string;
  direcoes: readonly OpcaoDirecao[];
}[] = [
  {
    valor: 'REUNIAO_COORDENACAO_EQUIPE',
    rotulo: 'Reunião entre coordenação e equipe técnica',
    direcoes: [],
  },
  { valor: 'FORMACAO_MULTIPROFISSIONAL', rotulo: 'Formação multiprofissional', direcoes: [] },
  { valor: 'REUNIAO_DIRECAO_GERAL', rotulo: 'Reunião com a direção geral', direcoes: [] },
  { valor: 'MONITORAMENTO_ORGAO_GESTOR', rotulo: 'Monitoramento do órgão gestor', direcoes: [] },
  {
    valor: 'VISITA_INSTITUCIONAL',
    rotulo: 'Visita institucional',
    direcoes: [
      { valor: 'REALIZADO', rotulo: 'Realizada pelo Centro-Dia' },
      { valor: 'RECEBIDO', rotulo: 'Recebida no Centro-Dia' },
    ],
  },
  {
    valor: 'EVENTO',
    rotulo: 'Evento',
    direcoes: [
      { valor: 'REALIZADO', rotulo: 'Realizado pelo Centro-Dia' },
      { valor: 'PARTICIPADO', rotulo: 'Centro-Dia participou' },
    ],
  },
  { valor: 'REUNIAO_PARCEIROS', rotulo: 'Reunião com parceiros', direcoes: [] },
];

export const ROTULOS_TIPO_EVENTO: Readonly<Record<TipoEvento, string>> = Object.fromEntries(
  TIPOS_EVENTO.map((t) => [t.valor, t.rotulo]),
) as Record<TipoEvento, string>;

/** Rótulo curto da indicação na lista, por tipo (a visita é "recebida", o evento "realizado"). */
export function rotuloDirecao(tipo: TipoEvento, direcao: DirecaoEvento | null): string | null {
  const opcao = TIPOS_EVENTO.find((t) => t.valor === tipo)?.direcoes.find(
    (d) => d.valor === direcao,
  );
  return opcao?.rotulo ?? null;
}
