import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { AtendidoBasico, AtendidoCompleto, PaginaAtendidos as Pagina } from './atendidos-api';
import { PaginaAtendidos } from './pagina-atendidos';

const COMPLETO: AtendidoCompleto = {
  completo: true,
  id: 'ins-1',
  pessoaId: 'pes-1',
  nomeCompleto: 'Ana Fictícia',
  situacao: 'ATIVA',
  turno: 'MATUTINO',
  dataInsercao: '2026-11-10',
  dataDesligamento: null,
  tipoDeficiencia: 'TEA',
  dataNascimento: '1996-01-01',
  idade: 30,
  faixaEtaria: 'ADULTO',
};

const BASICO: AtendidoBasico = {
  completo: false,
  id: 'ins-2',
  nomeCompleto: 'Bia Fictícia',
  situacao: 'DESLIGADA',
  turno: 'INTEGRAL',
  dataInsercao: '2026-10-01',
  dataDesligamento: '2026-11-01',
};

function pagina(...itens: (AtendidoBasico | AtendidoCompleto)[]): Pagina {
  return {
    content: itens,
    page: { size: 20, number: 0, totalElements: itens.length, totalPages: 1 },
  };
}

describe('PaginaAtendidos', () => {
  let http: HttpTestingController;

  function configurar(permissoes: Permissao[]) {
    TestBed.configureTestingModule({
      imports: [PaginaAtendidos],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes }));
    http = TestBed.inject(HttpTestingController);
  }

  afterEach(() => http.verify());

  async function criar() {
    const fixture = TestBed.createComponent(PaginaAtendidos);
    fixture.detectChanges();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('lista os ativos por padrão, com tipo e idade para quem lê a ficha completa', async () => {
    configurar(['ATENDIDOS_LER', 'ATENDIDOS_LER_BASICO']);
    const { fixture, elemento } = await criar();

    const pedido = http.expectOne((r) => r.url === '/api/atendidos');
    expect(pedido.request.params.get('situacao')).toBe('ATIVA');
    expect(pedido.request.params.get('page')).toBe('0');
    pedido.flush(pagina(COMPLETO));
    await fixture.whenStable();

    expect(elemento.textContent).toContain('Ana Fictícia');
    expect(elemento.textContent).toContain('Matutino');
    expect(elemento.textContent).toContain('30 anos');
    expect(elemento.textContent).toContain('Faixa etária');
    expect(elemento.querySelector('a.item')?.getAttribute('href')).toContain('ins-1');
  });

  it('esconde os filtros de dado completo e não os envia para o perfil básico', async () => {
    configurar(['ATENDIDOS_LER_BASICO']);
    const { fixture, elemento } = await criar();
    http.expectOne((r) => r.url === '/api/atendidos').flush(pagina(BASICO));
    await fixture.whenStable();

    expect(elemento.textContent).not.toContain('Tipo de deficiência');
    expect(elemento.textContent).not.toContain('Faixa etária');
    expect(elemento.textContent).toContain('Desligado');

    const componente = fixture.componentInstance as unknown as {
      filtros: { setValue(v: object): void };
      filtrar(): void;
    };
    componente.filtros.setValue({
      nome: ' Bia ',
      turno: 'INTEGRAL',
      situacao: null,
      tipoDeficiencia: 'TEA',
      faixaEtaria: 'JOVEM',
    });
    componente.filtrar();

    const pedido = http.expectOne((r) => r.url === '/api/atendidos');
    expect(pedido.request.params.get('nome')).toBe('Bia');
    expect(pedido.request.params.get('turno')).toBe('INTEGRAL');
    expect(pedido.request.params.has('situacao')).toBe(false);
    expect(pedido.request.params.has('tipoDeficiencia')).toBe(false);
    expect(pedido.request.params.has('faixaEtaria')).toBe(false);
    pedido.flush(pagina());
  });

  it('avisa quando ninguém atende aos filtros e quando a lista falha', async () => {
    configurar(['ATENDIDOS_LER_BASICO']);
    const { fixture, elemento } = await criar();
    http.expectOne((r) => r.url === '/api/atendidos').flush(pagina());
    await fixture.whenStable();
    expect(elemento.textContent).toContain('Ninguém com esses filtros');

    (fixture.componentInstance as unknown as { carregar(): void }).carregar();
    http
      .expectOne((r) => r.url === '/api/atendidos')
      .flush(null, { status: 500, statusText: 'Erro' });
    await fixture.whenStable();
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Não foi possível carregar',
    );
  });
});
