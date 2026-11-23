import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';

import { RelatorioEstatistico } from './estatisticas-api';
import { quadrimestreAnterior } from './periodo';
import { RelatorioPeriodo } from './relatorio-periodo';

const URL_QUADRIMESTRAL = '/api/estatisticas/relatorios/quadrimestral';
const URL_ANUAL = '/api/estatisticas/relatorios/anual';

const RELATORIO: RelatorioEstatistico = {
  titulo: 'Relatório quadrimestral de maio a agosto de 2026',
  inicio: '2026-05-01',
  fim: '2026-08-31',
  geradoEm: '2026-11-22T13:00:00Z',
  linhas: [
    { grupo: 'Lista de espera', indicador: 'Novas inscrições', quantidade: 9 },
    { grupo: 'Desligamentos', indicador: 'Evasão', quantidade: 0 },
  ],
};

describe('RelatorioPeriodo', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RelatorioPeriodo],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function renderizar() {
    const fixture = TestBed.createComponent(RelatorioPeriodo);
    fixture.detectChanges();
    const anterior = quadrimestreAnterior();
    const pedido = http.expectOne((r) => r.url === URL_QUADRIMESTRAL);
    expect(pedido.request.params.get('ano')).toBe(String(anterior.ano));
    expect(pedido.request.params.get('quadrimestre')).toBe(String(anterior.quadrimestre));
    pedido.flush(RELATORIO);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  interface Escolha {
    escolha: {
      setValue(v: { recorte: 'quadrimestral' | 'anual'; quadrimestre: number; ano: number }): void;
    };
  }

  it('abre no quadrimestre anterior e mostra os indicadores agrupados', async () => {
    const { elemento } = await renderizar();

    expect(elemento.querySelector('h2')?.textContent).toContain('maio a agosto de 2026');
    const grupos = Array.from(elemento.querySelectorAll('caption')).map((c) =>
      c.textContent?.trim(),
    );
    expect(grupos).toEqual(['Lista de espera', 'Desligamentos']);
  });

  it('gera o relatório anual do ano escolhido', async () => {
    const { fixture, elemento } = await renderizar();
    const componente = fixture.componentInstance as unknown as Escolha;

    componente.escolha.setValue({ recorte: 'anual', quadrimestre: 1, ano: 2024 });
    fixture.detectChanges();
    expect(elemento.querySelector('.quadrimestre')).toBeNull();
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();

    http
      .expectOne((r) => r.url === URL_ANUAL && r.params.get('ano') === '2024')
      .flush({ ...RELATORIO, titulo: 'Relatório anual de 2024' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(elemento.querySelector('h2')?.textContent).toContain('anual de 2024');
  });

  it('gera o quadrimestre escolhido', async () => {
    const { fixture, elemento } = await renderizar();
    const componente = fixture.componentInstance as unknown as Escolha;

    componente.escolha.setValue({ recorte: 'quadrimestral', quadrimestre: 1, ano: 2023 });
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();

    http
      .expectOne(
        (r) =>
          r.url === URL_QUADRIMESTRAL &&
          r.params.get('ano') === '2023' &&
          r.params.get('quadrimestre') === '1',
      )
      .flush(RELATORIO);
  });

  it('mostra o motivo quando o servidor recusa o período', async () => {
    const fixture = TestBed.createComponent(RelatorioPeriodo);
    fixture.detectChanges();
    http
      .expectOne((r) => r.url === URL_QUADRIMESTRAL)
      .flush(
        { title: 'Regra de negócio violada', detail: 'Só períodos passados ou o corrente.' },
        { status: 422, statusText: 'Unprocessable' },
      );
    await fixture.whenStable();
    fixture.detectChanges();

    const alerta = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alerta?.textContent).toContain('períodos passados');
  });
});
