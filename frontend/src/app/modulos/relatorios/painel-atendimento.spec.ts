import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';

import { PainelAtendimento as Painel } from './estatisticas-api';
import { PainelAtendimento } from './painel-atendimento';
import { dataIso, periodoPadrao } from './periodo';

const URL = '/api/estatisticas/dashboards/atendimento';

const PAINEL: Painel = {
  geradoEm: '2026-11-22T13:00:00Z',
  inicio: '2026-01-01',
  fim: '2026-11-22',
  violacoesPorCategoria: [
    { chave: 'ABUSO_SEXUAL', rotulo: 'Abuso sexual', total: 1 },
    { chave: 'OUTRAS', rotulo: 'Outras violações', total: 0 },
  ],
  atividadesPorTipo: [
    { chave: 'COM_USUARIOS', rotulo: 'Atividade com usuários', total: 3 },
    { chave: 'ESTUDO_DE_CASO', rotulo: 'Estudo de caso', total: 1 },
  ],
  totalAtividades: 4,
  atendidosNoPeriodo: 20,
  totalDesligamentos: 2,
  taxaDesligamento: 0.1,
  desligamentosPorMotivo: [
    { chave: 'EVASAO', rotulo: 'Evasão', total: 1, taxa: 0.05 },
    { chave: 'OUTROS', rotulo: 'Outros', total: 1, taxa: 0.05 },
  ],
};

describe('PainelAtendimento', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PainelAtendimento],
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

  async function renderizar(painel: Painel = PAINEL) {
    const fixture = TestBed.createComponent(PainelAtendimento);
    fixture.detectChanges();
    const padrao = periodoPadrao();
    const pedido = http.expectOne((r) => r.url === URL);
    expect(pedido.request.params.get('inicio')).toBe(dataIso(padrao.inicio));
    expect(pedido.request.params.get('fim')).toBe(dataIso(padrao.fim));
    pedido.flush(painel);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('abre com o ano corrente e mostra os totais e os três gráficos', async () => {
    const { elemento } = await renderizar();

    expect(elemento.querySelector('.numeros')?.textContent).toContain('10% dos atendidos');
    const titulos = Array.from(elemento.querySelectorAll('h2')).map((h) => h.textContent?.trim());
    expect(titulos).toEqual([
      'Violações de direitos por categoria',
      'Atividades realizadas por tipo',
      'Desligamentos por motivo',
    ]);
  });

  it('calcula a porcentagem dos desligamentos sobre os atendidos no período', async () => {
    const { elemento } = await renderizar();
    const desligamentos = elemento.querySelectorAll('app-grafico-barras')[2];

    expect(desligamentos.querySelector('.porcentagem')?.textContent?.trim()).toBe('5%');
  });

  it('não mostra violações para quem não as lê', async () => {
    const { elemento } = await renderizar({ ...PAINEL, violacoesPorCategoria: null });

    expect(elemento.querySelectorAll('app-grafico-barras').length).toBe(2);
    expect(elemento.textContent).not.toContain('Violações de direitos');
  });

  it('busca de novo com o período escolhido', async () => {
    const { elemento, fixture } = await renderizar();
    const componente = fixture.componentInstance as unknown as {
      periodo: { setValue(v: { inicio: Date; fim: Date }): void };
    };

    componente.periodo.setValue({ inicio: new Date(2026, 2, 1), fim: new Date(2026, 2, 31) });
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    const pedido = http.expectOne((r) => r.url === URL);

    expect(pedido.request.params.get('inicio')).toBe('2026-03-01');
    expect(pedido.request.params.get('fim')).toBe('2026-03-31');
    pedido.flush(PAINEL);
  });

  it('mostra a mensagem do servidor quando o período é recusado', async () => {
    const fixture = TestBed.createComponent(PainelAtendimento);
    fixture.detectChanges();
    http
      .expectOne((r) => r.url === URL)
      .flush(
        {
          title: 'Regra de negócio violada',
          detail: 'A data final não pode ser anterior à inicial.',
        },
        { status: 422, statusText: 'Unprocessable' },
      );
    await fixture.whenStable();
    fixture.detectChanges();

    const alerta = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alerta?.textContent).toContain('anterior à inicial');
  });
});
