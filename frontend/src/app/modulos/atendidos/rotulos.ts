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

/** Gêneros, com os nomes do enum `Genero` do backend (ADR 0143). Sem valor: não informado. */
export type Genero = 'FEMININO' | 'MASCULINO' | 'OUTRO';

// PENDENTE-CESJO(P21): opções de gênero e de escolaridade provisórias (ADR 0143).
export const GENEROS: readonly { valor: Genero; rotulo: string }[] = [
  { valor: 'FEMININO', rotulo: 'Feminino' },
  { valor: 'MASCULINO', rotulo: 'Masculino' },
  { valor: 'OUTRO', rotulo: 'Outro' },
];

/** Escolaridade, com os nomes do enum `Escolaridade` do backend (ADR 0143). */
export type Escolaridade =
  | 'SEM_ESCOLARIDADE'
  | 'FUNDAMENTAL_INCOMPLETO'
  | 'FUNDAMENTAL_COMPLETO'
  | 'MEDIO_INCOMPLETO'
  | 'MEDIO_COMPLETO'
  | 'SUPERIOR_INCOMPLETO'
  | 'SUPERIOR_COMPLETO';

export const ESCOLARIDADES: readonly { valor: Escolaridade; rotulo: string }[] = [
  { valor: 'SEM_ESCOLARIDADE', rotulo: 'Sem escolaridade' },
  { valor: 'FUNDAMENTAL_INCOMPLETO', rotulo: 'Fundamental incompleto' },
  { valor: 'FUNDAMENTAL_COMPLETO', rotulo: 'Fundamental completo' },
  { valor: 'MEDIO_INCOMPLETO', rotulo: 'Médio incompleto' },
  { valor: 'MEDIO_COMPLETO', rotulo: 'Médio completo' },
  { valor: 'SUPERIOR_INCOMPLETO', rotulo: 'Superior incompleto' },
  { valor: 'SUPERIOR_COMPLETO', rotulo: 'Superior completo' },
];

/** Crença religiosa, com as opções do RF007 (enum `CrencaReligiosa` do backend). */
export type CrencaReligiosa =
  'CATOLICA' | 'EVANGELICA' | 'TESTEMUNHA_DE_JEOVA' | 'MATRIZ_AFRICANA' | 'OUTRAS';

export const CRENCAS: readonly { valor: CrencaReligiosa; rotulo: string }[] = [
  { valor: 'CATOLICA', rotulo: 'Católica' },
  { valor: 'EVANGELICA', rotulo: 'Evangélica' },
  { valor: 'TESTEMUNHA_DE_JEOVA', rotulo: 'Testemunha de Jeová' },
  { valor: 'MATRIZ_AFRICANA', rotulo: 'Matriz africana' },
  { valor: 'OUTRAS', rotulo: 'Outras' },
];

/** Rótulo de um valor de uma das listas acima, ou "Não informado" sem valor. */
export function rotuloDe<T extends string>(
  lista: readonly { valor: T; rotulo: string }[],
  valor: T | null,
): string {
  return lista.find((item) => item.valor === valor)?.rotulo ?? 'Não informado';
}
