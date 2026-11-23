/** Data no formato `AAAA-MM-DD` do calendário local, sem passar por UTC. */
export function dataIso(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

/** Período padrão dos dashboards: de 1º de janeiro até hoje (ADR 0198). */
export function periodoPadrao(hoje: Date = new Date()): { inicio: Date; fim: Date } {
  const fim = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate());
  return { inicio: new Date(hoje.getFullYear(), 0, 1), fim };
}

/** Nomes dos meses, de janeiro (índice 0) a dezembro. */
export const MESES: readonly string[] = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
];

/** Mês no formato `AAAA-MM`, com `mes` de 1 a 12. */
export function mesIso(ano: number, mes: number): string {
  return `${ano}-${String(mes).padStart(2, '0')}`;
}

/** Mês padrão do relatório mensal: o anterior ao atual, que já fechou. */
export function mesAnterior(hoje: Date = new Date()): { ano: number; mes: number } {
  const data = new Date(hoje.getFullYear(), hoje.getMonth() - 1, 1);
  return { ano: data.getFullYear(), mes: data.getMonth() + 1 };
}

/** Primeiro ano oferecido na escolha do período dos relatórios. */
export const PRIMEIRO_ANO = 2020;

/** Anos da escolha do período dos relatórios, do corrente até {@link PRIMEIRO_ANO}. */
export function anosOferecidos(hoje: Date = new Date()): number[] {
  return Array.from(
    { length: hoje.getFullYear() - PRIMEIRO_ANO + 1 },
    (_, i) => hoje.getFullYear() - i,
  );
}

/** Quadrimestre (1 a 3) de um mês de 1 a 12: janeiro a abril é o 1º (ADR 0203). */
export function quadrimestreDoMes(mes: number): number {
  return Math.ceil(mes / 4);
}

/** Quadrimestre padrão: o anterior ao atual, que já fechou. */
export function quadrimestreAnterior(hoje: Date = new Date()): {
  ano: number;
  quadrimestre: number;
} {
  const atual = quadrimestreDoMes(hoje.getMonth() + 1);
  return atual === 1
    ? { ano: hoje.getFullYear() - 1, quadrimestre: 3 }
    : { ano: hoje.getFullYear(), quadrimestre: atual - 1 };
}
