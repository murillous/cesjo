/** Tipos de atividade do RF010, com os nomes do enum `TipoAtividade` do backend (ADR 0150). */
export type TipoAtividade = 'COM_FAMILIAS' | 'COM_USUARIOS' | 'COM_USUARIOS_E_FAMILIAS';

/** Tipos na ordem do documento de requisitos, para os campos de escolha. */
export const TIPOS_ATIVIDADE: readonly { valor: TipoAtividade; rotulo: string }[] = [
  { valor: 'COM_FAMILIAS', rotulo: 'Atividade com famílias' },
  { valor: 'COM_USUARIOS', rotulo: 'Atividade com usuários' },
  { valor: 'COM_USUARIOS_E_FAMILIAS', rotulo: 'Atividade com usuários e famílias' },
];

export const ROTULOS_TIPO_ATIVIDADE: Readonly<Record<TipoAtividade, string>> = Object.fromEntries(
  TIPOS_ATIVIDADE.map((t) => [t.valor, t.rotulo]),
) as Record<TipoAtividade, string>;
