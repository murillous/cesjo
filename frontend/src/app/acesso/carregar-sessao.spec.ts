import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { carregarSessao } from './carregar-sessao';
import { contaDeTeste } from './conta-de-teste';
import { Sessao } from './sessao';

describe('carregarSessao', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('define a conta devolvida por GET /api/sessao', async () => {
    const carregamento = TestBed.runInInjectionContext(() => carregarSessao());
    TestBed.inject(HttpTestingController).expectOne('/api/sessao').flush(contaDeTeste());
    await carregamento;

    expect(TestBed.inject(Sessao).conta()?.login).toBe('coordenacao');
  });

  it('começa sem sessão quando a API responde 401', async () => {
    const carregamento = TestBed.runInInjectionContext(() => carregarSessao());
    TestBed.inject(HttpTestingController)
      .expectOne('/api/sessao')
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    await carregamento;

    expect(TestBed.inject(Sessao).autenticada()).toBe(false);
  });
});
