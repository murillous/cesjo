import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { routes } from '../../app.routes';
import { dataIso, mesAnterior } from './periodo';
import { RelatorioIntervencao } from './relatorio-intervencao';
import { RelatorioIntervencao as Relatorio } from './relatorio-intervencao-api';

const URL = '/api/estatisticas/relatorios/intervencao';

const RELATORIO: Relatorio = {
  titulo: 'Relatório de intervenção de 01/10/2026 a 31/10/2026',
  inicio: '2026-10-01',
  fim: '2026-10-31',
  geradoEm: '2026-11-22T13:00:00Z',
  resumo: [
    { grupo: 'Casos', indicador: 'Usuários com violação identificada', quantidade: 1 },
    { grupo: 'Intervenções', indicador: 'Encaminhamentos enviados', quantidade: 0 },
  ],
  casos: [
    {
      usuario: 'Ana Fictícia',
      violacoes: [
        { categoria: 'Negligência ou abandono', ambito: null, identificadaEm: '2026-10-03' },
        {
          categoria: 'Violência física ou psicológica',
          ambito: 'Intrafamiliar',
          identificadaEm: '2026-10-10',
        },
      ],
      atendimentos: 2,
      ultimoAtendimento: '2026-10-20',
      situacao: 'Ativo no serviço',
    },
  ],
};

function entrarCom(permissoes: readonly Permissao[]): void {
  TestBed.inject(Sessao).definir({
    id: '00000000-0000-0000-0000-000000000001',
    login: 'conta.teste',
    nome: 'Conta de teste',
    perfil: 'COORDENADOR',
    permissoes,
    trocaSenhaObrigatoria: false,
  });
}

describe('RelatorioIntervencao', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
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
    const fixture = TestBed.createComponent(RelatorioIntervencao);
    fixture.detectChanges();
    await fixture.whenStable();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('não gera sozinho, porque cada geração é auditada', async () => {
    const { elemento } = await renderizar();

    http.expectNone((r) => r.url === URL);
    expect(elemento.textContent).toContain('registrados na auditoria');
  });

  it('gera o mês anterior e mostra os casos e o resumo', async () => {
    const { fixture, elemento } = await renderizar();
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();

    const anterior = mesAnterior();
    const pedido = http.expectOne((r) => r.url === URL);
    expect(pedido.request.params.get('inicio')).toBe(
      dataIso(new Date(anterior.ano, anterior.mes - 1, 1)),
    );
    expect(pedido.request.params.get('fim')).toBe(dataIso(new Date(anterior.ano, anterior.mes, 0)));
    pedido.flush(RELATORIO);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(elemento.querySelector('h2')?.textContent).toContain('01/10/2026 a 31/10/2026');
    const caso = elemento.querySelector('.caso');
    expect(caso?.querySelector('h4')?.textContent).toContain('Ana Fictícia');
    expect(caso?.textContent).toContain('Violência física ou psicológica, intrafamiliar');
    expect(caso?.textContent).toContain('identificada em 10/10/2026');
    expect(caso?.textContent).toContain('20/10/2026');
    expect(caso?.textContent).toContain('Ativo no serviço');
    const grupos = Array.from(elemento.querySelectorAll('caption')).map((c) =>
      c.textContent?.trim(),
    );
    expect(grupos).toEqual(['Casos', 'Intervenções']);
  });

  it('avisa quando não houve caso no período', async () => {
    const { fixture, elemento } = await renderizar();
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    http.expectOne((r) => r.url === URL).flush({ ...RELATORIO, casos: [] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(elemento.textContent).toContain('Nenhuma violação de direitos foi identificada');
  });

  it('mostra o motivo quando o servidor recusa o período', async () => {
    const { fixture, elemento } = await renderizar();
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    http
      .expectOne((r) => r.url === URL)
      .flush(
        { title: 'Regra de negócio violada', detail: 'O relatório de intervenção só pode ir até hoje.' },
        { status: 422, statusText: 'Unprocessable' },
      );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('até hoje');
  });

  it('esconde a aba de quem não lê violações', async () => {
    entrarCom(['RELATORIOS_LER']);

    const harness = await RouterTestingHarness.create('/relatorios/mensal');
    http.match(() => true).forEach((pedido) => pedido.flush({ linhas: [] }));

    expect(harness.routeNativeElement?.textContent).toContain('Relatório mensal');
    expect(harness.routeNativeElement?.textContent).not.toContain('Relatório de intervenção');
  });

  it('abre a aba para quem lê relatórios e violações', async () => {
    entrarCom(['RELATORIOS_LER', 'VIOLACOES_LER']);

    const harness = await RouterTestingHarness.create('/relatorios/intervencao');

    expect(harness.routeNativeElement?.textContent).toContain('Relatório de intervenção');
    expect(harness.routeNativeElement?.textContent).toContain('Gerar relatório');
  });

  it('não abre a rota para quem não lê violações', async () => {
    entrarCom(['RELATORIOS_LER']);

    const harness = await RouterTestingHarness.create('/relatorios/intervencao');

    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });
});
