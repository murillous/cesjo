/** Turnos, com os nomes do enum `Turno` do backend. */
export type Turno = 'MATUTINO' | 'VESPERTINO' | 'INTEGRAL';

/** Situação da inserção, com os nomes do enum `SituacaoInsercao` do backend. */
export type SituacaoInsercao = 'ATIVA' | 'DESLIGADA';

/** Na ordem em que aparecem nas telas. Rótulos iguais aos do backend. */
export const TURNOS: readonly { valor: Turno; rotulo: string; dica: string }[] = [
  { valor: 'MATUTINO', rotulo: 'Matutino', dica: 'Manhã' },
  { valor: 'VESPERTINO', rotulo: 'Vespertino', dica: 'Tarde' },
  { valor: 'INTEGRAL', rotulo: 'Integral', dica: 'Manhã e tarde' },
];

export const ROTULOS_SITUACAO_INSERCAO: Readonly<Record<SituacaoInsercao, string>> = {
  ATIVA: 'Ativo',
  DESLIGADA: 'Desligado',
};

export function rotuloTurno(turno: Turno): string {
  return TURNOS.find((t) => t.valor === turno)?.rotulo ?? turno;
}

/** Faixas etárias calculadas no backend (`FaixaEtaria`). */
export type FaixaEtaria = 'JOVEM' | 'ADULTO';

export const FAIXAS_ETARIAS: readonly { valor: FaixaEtaria; rotulo: string }[] = [
  { valor: 'JOVEM', rotulo: 'Jovem (18 a 29 anos)' },
  { valor: 'ADULTO', rotulo: 'Adulto (30 a 59 anos)' },
];

export function rotuloFaixa(faixa: FaixaEtaria | null): string {
  return FAIXAS_ETARIAS.find((f) => f.valor === faixa)?.rotulo ?? 'Fora de 18 a 59 anos';
}

/** Motivos de desligamento (RF008), com os nomes do enum `MotivoDesligamento` do backend. */
export type MotivoDesligamento =
  'EVASAO' | 'MUDANCA_MUNICIPIO' | 'SOLICITACAO_FAMILIA' | 'ALCANCE_OBJETIVOS' | 'OUTROS';

export const MOTIVOS_DESLIGAMENTO: readonly { valor: MotivoDesligamento; rotulo: string }[] = [
  { valor: 'EVASAO', rotulo: 'Evasão' },
  { valor: 'MUDANCA_MUNICIPIO', rotulo: 'Mudança para outro município' },
  { valor: 'SOLICITACAO_FAMILIA', rotulo: 'Solicitação da família' },
  { valor: 'ALCANCE_OBJETIVOS', rotulo: 'Alcance dos objetivos' },
  { valor: 'OUTROS', rotulo: 'Outros' },
];

export function rotuloMotivo(motivo: MotivoDesligamento): string {
  return MOTIVOS_DESLIGAMENTO.find((m) => m.valor === motivo)?.rotulo ?? motivo;
}

/** Categorias de violação de direitos do RF009, com os nomes do enum `TipoViolacao` do backend. */
export type TipoViolacao =
  | 'EXPLORACAO_IMAGEM'
  | 'ISOLAMENTO_SOCIAL'
  | 'CONFINAMENTO'
  | 'DISCRIMINACAO_FAMILIAR'
  | 'FALTA_CUIDADOS_CUIDADOR'
  | 'ESTRESSE_CUIDADOR'
  | 'DESVALORIZACAO_CAPACIDADE'
  | 'VIOLENCIA_PATRIMONIAL'
  | 'NEGLIGENCIA_ABANDONO'
  | 'ABUSO_SEXUAL'
  | 'VIOLENCIA_FISICA_PSICOLOGICA'
  | 'OUTRAS';

export type AmbitoViolacao = 'INTRAFAMILIAR' | 'EXTRAFAMILIAR';

/** Na ordem do RF009. `exigeAmbito` igual ao backend. */
export const TIPOS_VIOLACAO: readonly {
  valor: TipoViolacao;
  rotulo: string;
  exigeAmbito: boolean;
}[] = [
  { valor: 'EXPLORACAO_IMAGEM', rotulo: 'Exploração de imagem', exigeAmbito: false },
  { valor: 'ISOLAMENTO_SOCIAL', rotulo: 'Isolamento social', exigeAmbito: false },
  { valor: 'CONFINAMENTO', rotulo: 'Confinamento', exigeAmbito: false },
  {
    valor: 'DISCRIMINACAO_FAMILIAR',
    rotulo: 'Atitudes discriminatórias e preconceituosas no seio familiar',
    exigeAmbito: false,
  },
  {
    valor: 'FALTA_CUIDADOS_CUIDADOR',
    rotulo: 'Falta de cuidados adequados por parte do cuidador familiar',
    exigeAmbito: false,
  },
  {
    valor: 'ESTRESSE_CUIDADOR',
    rotulo: 'Alto grau de estresse do cuidador familiar',
    exigeAmbito: false,
  },
  {
    valor: 'DESVALORIZACAO_CAPACIDADE',
    rotulo: 'Desvalorização da potencialidade ou capacidade da pessoa',
    exigeAmbito: false,
  },
  { valor: 'VIOLENCIA_PATRIMONIAL', rotulo: 'Violência patrimonial', exigeAmbito: false },
  { valor: 'NEGLIGENCIA_ABANDONO', rotulo: 'Negligência ou abandono', exigeAmbito: false },
  { valor: 'ABUSO_SEXUAL', rotulo: 'Abuso sexual', exigeAmbito: true },
  {
    valor: 'VIOLENCIA_FISICA_PSICOLOGICA',
    rotulo: 'Violência física ou psicológica',
    exigeAmbito: true,
  },
  { valor: 'OUTRAS', rotulo: 'Outras violações', exigeAmbito: false },
];

export const AMBITOS_VIOLACAO: readonly { valor: AmbitoViolacao; rotulo: string }[] = [
  { valor: 'INTRAFAMILIAR', rotulo: 'Intrafamiliar' },
  { valor: 'EXTRAFAMILIAR', rotulo: 'Extrafamiliar' },
];

export function rotuloViolacao(tipo: TipoViolacao): string {
  return TIPOS_VIOLACAO.find((t) => t.valor === tipo)?.rotulo ?? tipo;
}

export function exigeAmbito(tipo: TipoViolacao | '' | null): boolean {
  return TIPOS_VIOLACAO.find((t) => t.valor === tipo)?.exigeAmbito ?? false;
}
