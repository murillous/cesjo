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
