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
