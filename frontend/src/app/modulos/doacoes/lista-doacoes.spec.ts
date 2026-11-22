import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { Pagina } from '../contas/contas-api';
import { doacaoFicticia } from './doacao-ficticia';
import { Doacao } from './doacoes-api';
import { ListaDoacoes } from './lista-doacoes';

interface Exposto {
  filtros: { patchValue(v: object): void };
  filtrar(): void;
}

function pagina(content: Doacao[]): Pagina<Doacao> {
  return { content, page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 } };
}

describe('ListaDoacoes', () => {
  let controle: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ListaDoacoes],
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
      contaDeTeste({ permissoes: ['DOACOES_LER', 'DOACOES_REGISTRAR'] }),
    );
  });

  afterEach(() => controle.verify());

  it('mostra direção, contraparte, tipo e quantidade ou valor', async () => {
    const fixture = TestBed.createComponent(ListaDoacoes);
    controle
      .expectOne((r) => r.url === '/api/doacoes')
      .flush(
        pagina([
          doacaoFicticia(),
          doacaoFicticia({
            id: 'd2',
            direcao: 'REALIZADA',
            contraparte: 'Família fictícia',
            tipo: 'DINHEIRO',
            quantidade: null,
            valor: 150.5,
          }),
        ]),
      );
    await fixture.whenStable();

    const linhas = (fixture.nativeElement as HTMLElement).querySelectorAll('a.linha');
    expect(linhas[0].textContent).toContain('Recebida');
    expect(linhas[0].textContent).toContain('Mercado Fictício');
    expect(linhas[0].textContent).toContain('5 cestas');
    expect(linhas[1].textContent).toContain('Realizada');
    expect(linhas[1].textContent).toContain('150');
  });

  it('lista por período e direção', async () => {
    const fixture = TestBed.createComponent(ListaDoacoes);
    controle.expectOne((r) => r.url === '/api/doacoes').flush(pagina([]));
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as Exposto;

    componente.filtros.patchValue({
      de: new Date(2026, 2, 1),
      ate: new Date(2026, 2, 31),
      direcao: 'RECEBIDA',
    });
    componente.filtrar();

    const pedido = controle.expectOne((r) => r.url === '/api/doacoes');
    expect(pedido.request.params.get('de')).toBe('2026-03-01');
    expect(pedido.request.params.get('ate')).toBe('2026-03-31');
    expect(pedido.request.params.get('direcao')).toBe('RECEBIDA');
    pedido.flush(pagina([]));
  });
});
