import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { ListaComunicacoes } from './lista-comunicacoes';

interface ListaExposta {
  filtros: { patchValue(v: object): void };
  filtrar(): void;
  limpar(): void;
}

const VAZIA = { content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } };

describe('ListaComunicacoes', () => {
  let controle: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ListaComunicacoes],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes: ['COMUNICACOES_LER'] }));
  });

  afterEach(() => controle.verify());

  it('filtra por instituição, tipo, direção e período e limpa os filtros', async () => {
    const fixture = TestBed.createComponent(ListaComunicacoes);
    await fixture.whenStable();
    controle.expectOne((r) => r.url === '/api/comunicacoes').flush(VAZIA);

    const lista = fixture.componentInstance as unknown as ListaExposta;
    lista.filtros.patchValue({
      tipo: 'OFICIO',
      direcao: 'ENVIADA',
      contraparte: ' CRAS ',
      inicio: new Date(2026, 10, 1),
      fim: new Date(2026, 10, 30),
    });
    lista.filtrar();

    const pedido = controle.expectOne((r) => r.url === '/api/comunicacoes');
    expect(pedido.request.params.get('tipo')).toBe('OFICIO');
    expect(pedido.request.params.get('direcao')).toBe('ENVIADA');
    expect(pedido.request.params.get('contraparte')).toBe('CRAS');
    expect(pedido.request.params.get('inicio')).toBe('2026-11-01');
    expect(pedido.request.params.get('fim')).toBe('2026-11-30');
    expect(pedido.request.params.get('page')).toBe('0');
    pedido.flush(VAZIA);
    await fixture.whenStable();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Nenhum registro com esses filtros');
    expect(texto).not.toContain('Registrar');

    lista.limpar();
    const limpo = controle.expectOne((r) => r.url === '/api/comunicacoes');
    expect(limpo.request.params.has('tipo')).toBe(false);
    expect(limpo.request.params.has('contraparte')).toBe(false);
    limpo.flush(VAZIA);
  });
});
