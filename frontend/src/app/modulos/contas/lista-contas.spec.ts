import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { Conta, Pagina } from './contas-api';
import { ListaContas } from './lista-contas';

export function contaFicticia(mudancas: Partial<Conta> = {}): Conta {
  return {
    id: '00000000-0000-0000-0000-0000000000a1',
    login: 'pessoa.ficticia',
    nome: 'Pessoa Fictícia',
    perfil: 'EQUIPE_TECNICA',
    ativa: true,
    bloqueada: false,
    trocaSenhaObrigatoria: false,
    criadoEm: '2026-11-20T10:00:00Z',
    atualizadoEm: '2026-11-20T10:00:00Z',
    ...mudancas,
  };
}

describe('ListaContas', () => {
  let controle: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ListaContas],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controle.verify());

  it('lista as contas com perfil e situação', async () => {
    const fixture = TestBed.createComponent(ListaContas);
    const pagina: Pagina<Conta> = {
      content: [
        contaFicticia(),
        contaFicticia({ id: 'b2', nome: 'Outra Pessoa', perfil: 'CONSULTA', ativa: false }),
      ],
      page: { size: 20, number: 0, totalElements: 2, totalPages: 1 },
    };
    const pedido = controle.expectOne((r) => r.url === '/api/contas');
    expect(pedido.request.params.get('page')).toBe('0');
    expect(pedido.request.params.get('size')).toBe('20');
    pedido.flush(pagina);
    await fixture.whenStable();

    const linhas = (fixture.nativeElement as HTMLElement).querySelectorAll('a.linha');
    expect(linhas).toHaveLength(2);
    expect(linhas[0].textContent).toContain('Equipe Técnica');
    expect(linhas[0].textContent).toContain('Ativa');
    expect(linhas[1].textContent).toContain('Desativada');
  });

  it('mostra o erro da API', async () => {
    const fixture = TestBed.createComponent(ListaContas);
    controle
      .expectOne((r) => r.url === '/api/contas')
      .flush({ detail: 'Sua conta não tem permissão.' }, { status: 403, statusText: 'Forbidden' });
    await fixture.whenStable();

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent,
    ).toContain('Sua conta não tem permissão.');
  });
});
