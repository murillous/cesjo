import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { FilaListaEspera } from './fila-lista-espera';
import { ItemFila, PaginaFila } from './lista-espera-api';

const URL = '/api/lista-espera/inscricoes';

function pagina(itens: ItemFila[], total = itens.length): PaginaFila {
  return { content: itens, page: { size: 20, number: 0, totalElements: total, totalPages: 1 } };
}

const COMPLETO: ItemFila = {
  id: 'i1',
  posicao: 1,
  nomeCompleto: 'Ana Fictícia',
  situacao: 'AGUARDANDO',
  dataCadastro: '2026-11-02',
  tipoDeficiencia: 'TEA',
  origemEncaminhamento: 'CRAS',
  cadastroCompleto: true,
};

const INCOMPLETO: ItemFila = {
  ...COMPLETO,
  id: 'i2',
  posicao: 2,
  nomeCompleto: 'Bruno Fictício',
  tipoDeficiencia: 'T21',
  origemEncaminhamento: 'SAUDE',
  cadastroCompleto: false,
};

interface Componente {
  filtros: { patchValue(v: object, opcoes?: { emitEvent: boolean }): void };
  buscar(): void;
  limpar(): void;
}

describe('FilaListaEspera', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FilaListaEspera],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http
      .match('/api/lista-espera/vagas')
      .forEach((pedido) =>
        pedido.flush({
          capacidade: 10,
          ocupadas: 4,
          disponiveis: 6,
          tamanhoFila: 2,
          definidaEm: null,
        }),
      );
    http.verify();
  });

  async function renderizar(itens: ItemFila[], permissoes?: Permissao[]) {
    TestBed.inject(Sessao).definir(contaDeTeste(permissoes ? { permissoes } : {}));
    const fixture = TestBed.createComponent(FilaListaEspera);
    fixture.detectChanges();
    const pedido = http.expectOne((r) => r.url === URL);
    pedido.flush(pagina(itens));
    await fixture.whenStable();
    return {
      fixture,
      pedido,
      elemento: fixture.nativeElement as HTMLElement,
      componente: fixture.componentInstance as unknown as Componente,
    };
  }

  it('lista os candidatos com a posição, em cartões e em tabela', async () => {
    const { elemento, pedido } = await renderizar([COMPLETO, INCOMPLETO]);

    expect(pedido.request.params.get('page')).toBe('0');
    expect(pedido.request.params.get('size')).toBe('20');
    const cartoes = elemento.querySelectorAll('.cartao');
    expect(cartoes.length).toBe(2);
    expect(cartoes[0].querySelector('.posicao')?.getAttribute('aria-label')).toBe('Posição 1');
    expect(cartoes[0].textContent).toContain('Ana Fictícia');
    expect(cartoes[0].textContent).toContain('Aguardando desde 02/11/2026');
    expect(cartoes[0].textContent).toContain('Encaminhado por CRAS');
    expect(cartoes[1].textContent).toContain('Síndrome de Down (T21)');
    expect(cartoes[1].querySelector('a.incompleto')?.getAttribute('href')).toBe('/i2/documentos');
    expect(elemento.querySelectorAll('.tabela tbody tr').length).toBe(2);
    expect(elemento.querySelector('.total')?.textContent).toContain('2 candidatos');
  });

  it('busca pelo nome e filtra por tipo, origem e situação a partir da primeira página', async () => {
    const { componente } = await renderizar([COMPLETO]);

    componente.filtros.patchValue(
      { nome: ' ana ', tipoDeficiencia: 'TEA', origem: 'CRAS', situacao: 'AGUARDANDO' },
      { emitEvent: false },
    );
    componente.buscar();
    const pedido = http.expectOne((r) => r.url === URL);
    expect(pedido.request.params.get('nome')).toBe('ana');
    expect(pedido.request.params.get('tipoDeficiencia')).toBe('TEA');
    expect(pedido.request.params.get('origem')).toBe('CRAS');
    expect(pedido.request.params.get('situacao')).toBe('AGUARDANDO');
    expect(pedido.request.params.get('page')).toBe('0');
    pedido.flush(pagina([]));
  });

  it('perfil Consulta vê só os dados básicos e não filtra por tipo nem origem', async () => {
    const basico: ItemFila = {
      id: 'i3',
      posicao: 3,
      nomeCompleto: 'Carla Fictícia',
      situacao: 'AGUARDANDO',
      dataCadastro: '2026-11-03',
    };
    const { elemento, componente } = await renderizar(
      [basico],
      ['NOTIFICACOES_LER', 'LISTA_ESPERA_FILA_LER'],
    );

    expect(elemento.querySelectorAll('.selecoes mat-select').length).toBe(1);
    expect(elemento.querySelector('.cadastrar')).toBeNull();
    expect(elemento.querySelector('.cartao a')).toBeNull();
    expect(elemento.querySelectorAll('.tabela thead th').length).toBe(4);

    componente.filtros.patchValue(
      { tipoDeficiencia: 'TEA', origem: 'CRAS' },
      {
        emitEvent: false,
      },
    );
    componente.buscar();
    const pedido = http.expectOne((r) => r.url === URL);
    expect(pedido.request.params.has('tipoDeficiencia')).toBe(false);
    expect(pedido.request.params.has('origem')).toBe(false);
    pedido.flush(pagina([basico]));
  });

  it('mostra a fila vazia com o convite para cadastrar', async () => {
    const { elemento } = await renderizar([]);
    expect(elemento.querySelector('.aviso')?.textContent).toContain('A fila está vazia.');
    expect(elemento.querySelector('.aviso a')?.textContent).toContain(
      'Cadastrar o primeiro candidato',
    );
  });

  it('mostra o erro com a opção de tentar de novo', async () => {
    TestBed.inject(Sessao).definir(contaDeTeste());
    const fixture = TestBed.createComponent(FilaListaEspera);
    fixture.detectChanges();
    http.expectOne((r) => r.url === URL).flush(null, { status: 500, statusText: 'Erro' });
    await fixture.whenStable();

    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Não foi possível carregar a fila.',
    );
    elemento.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    http.expectOne((r) => r.url === URL).flush(pagina([COMPLETO]));
  });
});
