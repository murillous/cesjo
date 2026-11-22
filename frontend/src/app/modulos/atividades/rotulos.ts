/** Tipos de atividade do RF010, com os nomes do enum `TipoAtividade` do backend (ADR 0150, ADR 0154). */
export type TipoAtividade =
  | 'COM_FAMILIAS'
  | 'COM_USUARIOS'
  | 'COM_USUARIOS_E_FAMILIAS'
  | 'ATENDIMENTO_MULTIPROFISSIONAL'
  | 'ATENDIMENTO_TECNICO_INDIVIDUALIZADO'
  | 'ESTUDO_DE_CASO';

export interface DescricaoTipo {
  valor: TipoAtividade;
  rotulo: string;
  /** Atendimento ou estudo de caso: um só usuário e leitura auditada. */
  individual: boolean;
  /** Mínimo de profissionais. O backend confere a mesma regra em `TipoAtividade`. */
  minimoProfissionais: number;
}

/** Tipos na ordem do documento de requisitos, para os campos de escolha. */
export const TIPOS_ATIVIDADE: readonly DescricaoTipo[] = [
  {
    valor: 'COM_FAMILIAS',
    rotulo: 'Atividade com famílias',
    individual: false,
    minimoProfissionais: 1,
  },
  {
    valor: 'COM_USUARIOS',
    rotulo: 'Atividade com usuários',
    individual: false,
    minimoProfissionais: 1,
  },
  {
    valor: 'COM_USUARIOS_E_FAMILIAS',
    rotulo: 'Atividade com usuários e famílias',
    individual: false,
    minimoProfissionais: 1,
  },
  // PENDENTE-CESJO(P25): o atendimento multiprofissional exige pelo menos dois profissionais.
  {
    valor: 'ATENDIMENTO_MULTIPROFISSIONAL',
    rotulo: 'Atendimento multiprofissional',
    individual: true,
    minimoProfissionais: 2,
  },
  {
    valor: 'ATENDIMENTO_TECNICO_INDIVIDUALIZADO',
    rotulo: 'Atendimento técnico individualizado',
    individual: true,
    minimoProfissionais: 1,
  },
  { valor: 'ESTUDO_DE_CASO', rotulo: 'Estudo de caso', individual: true, minimoProfissionais: 1 },
];

export const ROTULOS_TIPO_ATIVIDADE: Readonly<Record<TipoAtividade, string>> = Object.fromEntries(
  TIPOS_ATIVIDADE.map((t) => [t.valor, t.rotulo]),
) as Record<TipoAtividade, string>;

/** Descrição do tipo; tipo desconhecido conta como coletivo. */
export function descricaoTipo(tipo: TipoAtividade | ''): DescricaoTipo | undefined {
  return TIPOS_ATIVIDADE.find((t) => t.valor === tipo);
}
