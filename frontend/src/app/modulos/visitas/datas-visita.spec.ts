import { dataIso, horaDe, inicioDaSemana, lerDataIso } from './datas-visita';

describe('datas da agenda de visitas', () => {
  it('começa a semana na segunda-feira', () => {
    expect(dataIso(inicioDaSemana(new Date(2026, 10, 26)))).toBe('2026-11-23');
    expect(dataIso(inicioDaSemana(new Date(2026, 10, 29)))).toBe('2026-11-23');
    expect(dataIso(inicioDaSemana(new Date(2026, 10, 23)))).toBe('2026-11-23');
  });

  it('lê datas ISO como data local e recusa texto inválido', () => {
    expect(lerDataIso('2026-11-25')?.getDate()).toBe(25);
    expect(lerDataIso('25/11/2026')).toBeNull();
    expect(lerDataIso(undefined)).toBeNull();
  });

  it('tira a hora sem aplicar fuso', () => {
    expect(horaDe('2026-11-25T08:05:00')).toBe('08:05');
  });
});
