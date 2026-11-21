/** Tipos de deficiência, com os nomes do enum `TipoDeficiencia` do backend. */
export type TipoDeficiencia = 'FISICA' | 'SENSORIAL' | 'INTELECTUAL' | 'MULTIPLA' | 'TEA' | 'T21';

/** Origens de encaminhamento, com os nomes do enum `OrigemEncaminhamento` do backend. */
export type OrigemEncaminhamento =
  'CRAS' | 'CREAS' | 'JUDICIARIO' | 'SAUDE' | 'EDUCACAO' | 'DEMANDA_ESPONTANEA';

export type SituacaoInscricao = 'AGUARDANDO' | 'INSERIDO';

/** Na ordem em que aparecem no formulário. Rótulos iguais aos do backend. */
export const TIPOS_DEFICIENCIA: readonly { valor: TipoDeficiencia; rotulo: string }[] = [
  { valor: 'FISICA', rotulo: 'Física' },
  { valor: 'SENSORIAL', rotulo: 'Sensorial' },
  { valor: 'INTELECTUAL', rotulo: 'Intelectual' },
  { valor: 'MULTIPLA', rotulo: 'Múltipla' },
  { valor: 'TEA', rotulo: 'TEA' },
  { valor: 'T21', rotulo: 'Síndrome de Down (T21)' },
];

export const ORIGENS_ENCAMINHAMENTO: readonly { valor: OrigemEncaminhamento; rotulo: string }[] = [
  { valor: 'CRAS', rotulo: 'CRAS' },
  { valor: 'CREAS', rotulo: 'CREAS' },
  { valor: 'JUDICIARIO', rotulo: 'Judiciário' },
  { valor: 'SAUDE', rotulo: 'Saúde' },
  { valor: 'EDUCACAO', rotulo: 'Educação' },
  { valor: 'DEMANDA_ESPONTANEA', rotulo: 'Demanda espontânea' },
];

/** Documentos da pessoa, com os nomes do enum `TipoDocumento` do backend. */
export type TipoDocumento = 'LAUDO_MEDICO' | 'COMPROVANTE_RESIDENCIA';

/** Os dois documentos que o RF001 exige, na ordem em que aparecem nas telas (ADR 0056). */
export const DOCUMENTOS_OBRIGATORIOS: readonly { valor: TipoDocumento; rotulo: string }[] = [
  { valor: 'LAUDO_MEDICO', rotulo: 'Laudo médico' },
  { valor: 'COMPROVANTE_RESIDENCIA', rotulo: 'Comprovante de residência' },
];
