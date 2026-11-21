/** Data local no formato `AAAA-MM-DD`, como o backend espera para `LocalDate`. */
export function dataIso(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

/** Hoje, à meia-noite local. */
export function hoje(): Date {
  const agora = new Date();
  return new Date(agora.getFullYear(), agora.getMonth(), agora.getDate());
}

/** Data local a partir de `AAAA-MM-DD`, sem passar por UTC (que mudaria o dia). */
export function dataDeIso(texto: string): Date {
  const [ano, mes, dia] = texto.split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}
