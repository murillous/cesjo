import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { LinhaViolacao, PaginaViolacoes } from './atendidos-api';
import { PainelViolacoes } from './painel-violacoes';

const VIOLACAO: LinhaViolacao = {
  id: 'vio-1',
  insercaoId: 'ins-1',
  pessoaId: 'pes-1',
  nomeCompleto: 'Ana Fictícia',
  tipo: 'ABUSO_SEXUAL',
  ambito: 'INTRAFAMILIAR',
  dataIdentificacao: '2026-11-03',
};

interface Componente {
  filtros: { setValue(v: object): void };
  filtrar(): void;
}

function pagina(...itens: LinhaViolacao[]): PaginaViolacoes {
  return {
    content: itens,
    page: { size: 20, number: 0, totalElements: itens.length, totalPages: 1 },
  };
}

describe('PainelViolacoes', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PainelViolacoes],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function criar(insercaoId: string | null) {
    const fixture = TestBed.createComponent(PainelViolacoes);
    fixture.componentRef.setInput('insercaoId', insercaoId);
    fixture.detectChanges();
    return {
      fixture,
      componente: fixture.componentInstance as unknown as Componente,
      elemento: fixture.nativeElement as HTMLElement,
    };
  }

  it('na ficha, lista as violações da pessoa sem o nome e sem buscar a descrição', async () => {
    const { fixture, elemento } = await criar('ins-1');
    http.expectOne('/api/atendidos/ins-1/violacoes').flush([VIOLACAO]);
    await fixture.whenStable();

    expect(elemento.querySelector('.categoria')?.textContent).toContain('Abuso sexual');
    expect(elemento.querySelector('.ambito')?.textContent).toContain('Intrafamiliar');
    expect(elemento.querySelector('.data')?.textContent).toContain('03/11/2026');
    expect(elemento.querySelector('.pessoa')).toBeNull();
    expect(elemento.querySelector('mat-paginator')).toBeNull();
  });

  it('lê a descrição só quando pedem, uma vez, e esconde de novo', async () => {
    const { fixture, elemento } = await criar('ins-1');
    http.expectOne('/api/atendidos/ins-1/violacoes').flush([VIOLACAO]);
    await fixture.whenStable();
    const botao = elemento.querySelector<HTMLButtonElement>('button.ler')!;
    expect(botao.getAttribute('aria-expanded')).toBe('false');

    botao.click();
    http
      .expectOne('/api/violacoes/vio-1')
      .flush({ ...VIOLACAO, descricao: 'Relato fictício.', registradaEm: '' });
    await fixture.whenStable();
    expect(elemento.querySelector('.descricao .texto')?.textContent).toContain('Relato fictício.');
    expect(botao.getAttribute('aria-expanded')).toBe('true');
    expect(botao.textContent).toContain('Esconder descrição');

    botao.click();
    await fixture.whenStable();
    expect(elemento.querySelector('.descricao .texto')).toBeNull();
    http.expectNone('/api/violacoes/vio-1');
  });

  it('na lista geral, mostra o nome com link para a ficha e pagina', async () => {
    const { fixture, elemento } = await criar(null);
    http
      .expectOne((r) => r.url === '/api/violacoes' && r.params.get('page') === '0')
      .flush(pagina(VIOLACAO));
    await fixture.whenStable();

    const pessoa = elemento.querySelector('a.pessoa');
    expect(pessoa?.textContent).toContain('Ana Fictícia');
    expect(pessoa?.getAttribute('href')).toBe('/atendidos/ins-1');
    expect(elemento.querySelector('mat-paginator')).not.toBeNull();
  });

  it('filtra por categoria e período', async () => {
    const { fixture, componente } = await criar(null);
    http.expectOne((r) => r.url === '/api/violacoes').flush(pagina());
    await fixture.whenStable();

    componente.filtros.setValue({
      tipo: 'CONFINAMENTO',
      de: new Date(2026, 0, 1),
      ate: new Date(2026, 5, 30),
    });
    componente.filtrar();
    const pedido = http.expectOne((r) => r.url === '/api/violacoes');
    expect(pedido.request.params.get('tipo')).toBe('CONFINAMENTO');
    expect(pedido.request.params.get('de')).toBe('2026-01-01');
    expect(pedido.request.params.get('ate')).toBe('2026-06-30');
    pedido.flush(pagina());
  });

  it('não busca com o período invertido', async () => {
    const { fixture, componente, elemento } = await criar(null);
    http.expectOne((r) => r.url === '/api/violacoes').flush(pagina());
    await fixture.whenStable();

    componente.filtros.setValue({
      tipo: null,
      de: new Date(2026, 5, 1),
      ate: new Date(2026, 0, 1),
    });
    componente.filtrar();
    await fixture.whenStable();
    http.expectNone((r) => r.url === '/api/violacoes');
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('antes do fim');
  });

  it('avisa quando não há violação com os filtros', async () => {
    const { fixture, elemento } = await criar('ins-1');
    http.expectOne('/api/atendidos/ins-1/violacoes').flush([]);
    await fixture.whenStable();
    expect(elemento.textContent).toContain('Nenhuma violação registrada');
  });
});
