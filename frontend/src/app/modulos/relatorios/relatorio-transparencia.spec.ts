import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';

import { dataIso } from './periodo';
import { RelatorioTransparencia } from './relatorio-transparencia';
import { anoAnterior, RelatorioTransparencia as Relatorio } from './transparencia-api';

const URL = '/api/estatisticas/relatorios/transparencia';

const RELATORIO: Relatorio = {
  titulo: 'Relatório de transparência de 2025',
  inicio: '2025-01-01',
  fim: '2025-12-31',
  geradoEm: '2026-11-22T13:00:00Z',
  linhas: [
    {
      grupo: 'Usuários atendidos',
      indicador: 'Usuários atendidos no período',
      valor: 31,
      unidade: 'QUANTIDADE',
    },
    { grupo: 'Doações recebidas', indicador: 'Alimentos', valor: 0, unidade: 'QUANTIDADE' },
    {
      grupo: 'Doações recebidas',
      indicador: 'Valor recebido em dinheiro',
      valor: 1750.5,
      unidade: 'REAIS',
    },
  ],
};

describe('RelatorioTransparencia', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RelatorioTransparencia],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function renderizar() {
    const fixture = TestBed.createComponent(RelatorioTransparencia);
    fixture.detectChanges();
    const padrao = anoAnterior();
    const pedido = http.expectOne((r) => r.url === URL);
    expect(pedido.request.params.get('inicio')).toBe(dataIso(padrao.inicio));
    expect(pedido.request.params.get('fim')).toBe(dataIso(padrao.fim));
    pedido.flush(RELATORIO);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('abre no ano anterior e mostra os números agrupados, com o dinheiro em reais', async () => {
    const { elemento } = await renderizar();

    expect(elemento.querySelector('h2')?.textContent).toContain('transparência de 2025');
    const grupos = Array.from(elemento.querySelectorAll('caption')).map((c) =>
      c.textContent?.trim(),
    );
    expect(grupos).toEqual(['Usuários atendidos', 'Doações recebidas']);
    expect(elemento.querySelector('.zero')?.textContent).toContain('Alimentos');
    expect(elemento.querySelector('.reais td')?.textContent).toMatch(/R\$\s?1[.,]?750[.,]50/);
    expect(elemento.querySelector('.publico')?.textContent).toContain('sem nome');
  });

  it('gera o período escolhido', async () => {
    const { fixture, elemento } = await renderizar();
    const componente = fixture.componentInstance as unknown as {
      periodo: { setValue(v: { inicio: Date; fim: Date }): void };
    };

    componente.periodo.setValue({ inicio: new Date(2024, 0, 1), fim: new Date(2024, 5, 30) });
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();

    http
      .expectOne(
        (r) =>
          r.url === URL &&
          r.params.get('inicio') === '2024-01-01' &&
          r.params.get('fim') === '2024-06-30',
      )
      .flush(RELATORIO);
  });

  it('mostra o motivo quando o servidor recusa o período', async () => {
    const fixture = TestBed.createComponent(RelatorioTransparencia);
    fixture.detectChanges();
    http
      .expectOne((r) => r.url === URL)
      .flush(
        {
          title: 'Regra de negócio violada',
          detail: 'O relatório de transparência só pode ir até hoje.',
        },
        { status: 422, statusText: 'Unprocessable' },
      );
    await fixture.whenStable();
    fixture.detectChanges();

    const alerta = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alerta?.textContent).toContain('até hoje');
  });
});
