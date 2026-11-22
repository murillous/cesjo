import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { Pagina } from '../contas/contas-api';
import { ListaParceiros } from './lista-parceiros';
import { parceiroFicticio } from './parceiro-ficticio';
import { Parceiro } from './parcerias-api';

function pagina(content: Parceiro[]): Pagina<Parceiro> {
  return { content, page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 } };
}

describe('ListaParceiros', () => {
  let controle: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ListaParceiros],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    TestBed.inject(Sessao).definir(
      contaDeTeste({ permissoes: ['PARCERIAS_LER', 'PARCERIAS_GERENCIAR'] }),
    );
  });

  afterEach(() => controle.verify());

  it('começa pelos ativos e mostra tipo e vigência', async () => {
    const fixture = TestBed.createComponent(ListaParceiros);
    const pedido = controle.expectOne((r) => r.url === '/api/parceiros');
    expect(pedido.request.params.get('ativo')).toBe('true');
    pedido.flush(
      pagina([
        parceiroFicticio(),
        parceiroFicticio({
          id: 'p2',
          nome: 'Universidade Fictícia',
          tipo: 'INSTITUICAO_ENSINO',
          vigenciaInicio: '2026-01-01',
          vigenciaFim: '2027-12-31',
        }),
      ]),
    );
    await fixture.whenStable();

    const linhas = (fixture.nativeElement as HTMLElement).querySelectorAll('a.linha');
    expect(linhas).toHaveLength(2);
    expect(linhas[0].textContent).toContain('Órgão público');
    expect(linhas[0].textContent).toContain('Sem vigência informada');
    expect(linhas[1].textContent).toContain('01/01/2026');
    expect(linhas[1].textContent).toContain('31/12/2027');
  });

  it('mostra o convite ao cadastro quando não há parceiros', async () => {
    const fixture = TestBed.createComponent(ListaParceiros);
    controle.expectOne((r) => r.url === '/api/parceiros').flush(pagina([]));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Novo parceiro');
  });
});
