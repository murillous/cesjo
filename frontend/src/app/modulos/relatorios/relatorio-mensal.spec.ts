import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';

import { RelatorioEstatistico } from './estatisticas-api';
import { mesAnterior, mesIso } from './periodo';
import { RelatorioMensal } from './relatorio-mensal';

const URL = '/api/estatisticas/relatorios/mensal';

const RELATORIO: RelatorioEstatistico = {
  titulo: 'Relatório mensal de outubro de 2026',
  inicio: '2026-10-01',
  fim: '2026-10-31',
  geradoEm: '2026-11-22T13:00:00Z',
  linhas: [
    { grupo: 'Lista de espera', indicador: 'Novas inscrições', quantidade: 4 },
    { grupo: 'Lista de espera', indicador: 'Candidatos na fila no último dia', quantidade: 12 },
    { grupo: 'Desligamentos', indicador: 'Evasão', quantidade: 0 },
  ],
};

describe('RelatorioMensal', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RelatorioMensal],
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
    const fixture = TestBed.createComponent(RelatorioMensal);
    fixture.detectChanges();
    const anterior = mesAnterior();
    const pedido = http.expectOne((r) => r.url === URL);
    expect(pedido.request.params.get('mes')).toBe(mesIso(anterior.ano, anterior.mes));
    pedido.flush(RELATORIO);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('abre no mês anterior e mostra os indicadores agrupados', async () => {
    const { elemento } = await renderizar();

    expect(elemento.querySelector('h2')?.textContent).toContain('outubro de 2026');
    const grupos = Array.from(elemento.querySelectorAll('caption')).map((c) =>
      c.textContent?.trim(),
    );
    expect(grupos).toEqual(['Lista de espera', 'Desligamentos']);
    const primeiraTabela = elemento.querySelectorAll('table')[0];
    expect(primeiraTabela.querySelectorAll('tbody tr').length).toBe(2);
    expect(elemento.querySelector('.zero')?.textContent).toContain('Evasão');
  });

  it('gera o mês escolhido', async () => {
    const { fixture, elemento } = await renderizar();
    const componente = fixture.componentInstance as unknown as {
      escolha: { setValue(v: { mes: number; ano: number }): void };
    };

    componente.escolha.setValue({ mes: 3, ano: 2024 });
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();

    http.expectOne((r) => r.url === URL && r.params.get('mes') === '2024-03').flush(RELATORIO);
  });

  it('mostra o motivo quando o servidor recusa o mês', async () => {
    const fixture = TestBed.createComponent(RelatorioMensal);
    fixture.detectChanges();
    http
      .expectOne((r) => r.url === URL)
      .flush(
        {
          title: 'Regra de negócio violada',
          detail: 'O relatório só pode ser gerado para meses passados.',
        },
        { status: 422, statusText: 'Unprocessable' },
      );
    await fixture.whenStable();
    fixture.detectChanges();

    const alerta = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alerta?.textContent).toContain('meses passados');
  });
});
