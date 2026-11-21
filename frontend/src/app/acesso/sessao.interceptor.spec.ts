import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from './conta-de-teste';
import { Sessao } from './sessao';
import { sessaoInterceptor } from './sessao.interceptor';

describe('sessaoInterceptor', () => {
  let http: HttpClient;
  let controle: HttpTestingController;
  let router: Router;
  let sessao: Sessao;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([sessaoInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controle = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    sessao = TestBed.inject(Sessao);
    sessao.definir(contaDeTeste());
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => controle.verify());

  it('em 401 limpa a sessão e leva para a tela de entrada', () => {
    http.get('/api/contas').subscribe({ error: () => undefined });
    controle.expectOne('/api/contas').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(sessao.autenticada()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/entrar'], { queryParams: {} });
  });

  it('não redireciona no 401 do carregamento da sessão nem do login', () => {
    http.get('/api/sessao').subscribe({ error: () => undefined });
    controle.expectOne('/api/sessao').flush(null, { status: 401, statusText: 'Unauthorized' });
    http.post('/api/sessao/entrar', {}).subscribe({ error: () => undefined });
    controle
      .expectOne('/api/sessao/entrar')
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('em 403 de troca de senha pendente leva para a tela de troca', () => {
    http.get('/api/contas').subscribe({ error: () => undefined });
    controle
      .expectOne('/api/contas')
      .flush({ title: 'Troca de senha obrigatória' }, { status: 403, statusText: 'Forbidden' });

    expect(router.navigate).toHaveBeenCalledWith(['/trocar-senha']);
    expect(sessao.autenticada()).toBe(true);
  });

  it('em 403 comum não redireciona', () => {
    http.get('/api/contas').subscribe({ error: () => undefined });
    controle
      .expectOne('/api/contas')
      .flush({ title: 'Acesso negado' }, { status: 403, statusText: 'Forbidden' });

    expect(router.navigate).not.toHaveBeenCalled();
  });
});
