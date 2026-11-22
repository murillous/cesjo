import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { Pagina } from '../contas/contas-api';
import { eventoFicticio } from './evento-ficticio';
import { EventoInstitucional } from './eventos-api';
import { ListaEventos } from './lista-eventos';

interface Exposto {
  filtros: { patchValue(v: object): void };
  filtrar(): void;
}

function pagina(content: EventoInstitucional[]): Pagina<EventoInstitucional> {
  return { content, page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 } };
}

describe('ListaEventos', () => {
  let controle: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ListaEventos],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    TestBed.inject(Sessao).definir(
      contaDeTeste({ permissoes: ['EVENTOS_LER', 'EVENTOS_REGISTRAR'] }),
    );
  });

  afterEach(() => controle.verify());

  it('mostra tipo, indicação e parceiro de cada registro', async () => {
    const fixture = TestBed.createComponent(ListaEventos);
    const pedido = controle.expectOne((r) => r.url === '/api/eventos-institucionais');
    expect(pedido.request.params.has('tipo')).toBe(false);
    pedido.flush(
      pagina([
        eventoFicticio({
          tipo: 'VISITA_INSTITUCIONAL',
          direcao: 'RECEBIDO',
          parceiro: { id: 'p1', nome: 'CREAS Fictício' },
        }),
      ]),
    );
    await fixture.whenStable();

    const linha = (fixture.nativeElement as HTMLElement).querySelector('a.linha')?.textContent;
    expect(linha).toContain('02/03/2026');
    expect(linha).toContain('Visita institucional');
    expect(linha).toContain('Recebida no Centro-Dia');
    expect(linha).toContain('CREAS Fictício');
  });

  it('filtra por tipo e período', async () => {
    const fixture = TestBed.createComponent(ListaEventos);
    controle.expectOne((r) => r.url === '/api/eventos-institucionais').flush(pagina([]));
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as Exposto;

    componente.filtros.patchValue({
      tipo: 'EVENTO',
      de: new Date(2026, 0, 1),
      ate: new Date(2026, 0, 31),
    });
    componente.filtrar();

    const pedido = controle.expectOne((r) => r.url === '/api/eventos-institucionais');
    expect(pedido.request.params.get('tipo')).toBe('EVENTO');
    expect(pedido.request.params.get('de')).toBe('2026-01-01');
    expect(pedido.request.params.get('ate')).toBe('2026-01-31');
    pedido.flush(pagina([]));
  });
});
