import { dataIso } from '../lista-espera/datas';

/** Segunda-feira da semana de `data`, à meia-noite local. A semana da agenda vai de segunda a domingo. */
export function inicioDaSemana(data: Date): Date {
  const dia = new Date(data.getFullYear(), data.getMonth(), data.getDate());
  const deslocamento = (dia.getDay() + 6) % 7;
  dia.setDate(dia.getDate() - deslocamento);
  return dia;
}

export function somarDias(data: Date, dias: number): Date {
  const nova = new Date(data);
  nova.setDate(nova.getDate() + dias);
  return nova;
}

/** Lê `AAAA-MM-DD` como data local. Devolve `null` se o texto não for uma data. */
export function lerDataIso(texto: string | null | undefined): Date | null {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(texto ?? '');
  if (!partes) {
    return null;
  }
  const data = new Date(Number(partes[1]), Number(partes[2]) - 1, Number(partes[3]));
  return Number.isNaN(data.getTime()) ? null : data;
}

/** Parte da data (`AAAA-MM-DD`) de um `dataHora` local. */
export function diaDe(dataHora: string): string {
  return dataHora.slice(0, 10);
}

/** Hora `HH:mm` de um `dataHora` local, sem passar por `Date` (que aplicaria fuso). */
export function horaDe(dataHora: string): string {
  return dataHora.slice(11, 16);
}

export { dataIso };
