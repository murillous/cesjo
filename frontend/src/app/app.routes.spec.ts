import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { contaDeTeste } from './acesso/conta-de-teste';
import { Sessao } from './acesso/sessao';
import { routes } from './app.routes';

describe('Rotas', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  describe('com sessão', () => {
    beforeEach(() => TestBed.inject(Sessao).definir(contaDeTeste()));

    it('carrega a página inicial na raiz', async () => {
      const harness = await RouterTestingHarness.create('/');

      expect(harness.routeNativeElement?.textContent).toContain('Sistema de Gestão do Centro-Dia');
    });

    it('mostra a página 404 para endereço inexistente', async () => {
      const harness = await RouterTestingHarness.create('/nao-existe');

      expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
    });

    it('leva quem já entrou da tela de entrada para o início', async () => {
      await RouterTestingHarness.create('/entrar');

      expect(TestBed.inject(Router).url).toBe('/');
    });

    it('esconde o módulo de contas de quem não tem a permissão', async () => {
      TestBed.inject(Sessao).definir(
        contaDeTeste({ perfil: 'CONSULTA', permissoes: ['NOTIFICACOES_LER'] }),
      );
      const harness = await RouterTestingHarness.create('/contas');

      expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
    });
  });

  it('sem sessão, leva para a tela de entrada guardando o destino', async () => {
    const harness = await RouterTestingHarness.create('/contas');

    expect(TestBed.inject(Router).url).toBe('/entrar?retorno=%2Fcontas');
    expect(harness.routeNativeElement?.textContent).toContain('Entrar');
  });

  it('com troca de senha pendente, só mostra a tela de troca', async () => {
    TestBed.inject(Sessao).definir(contaDeTeste({ trocaSenhaObrigatoria: true }));
    const harness = await RouterTestingHarness.create('/');

    expect(TestBed.inject(Router).url).toBe('/trocar-senha');
    expect(harness.routeNativeElement?.textContent).toContain('Crie a sua senha');
  });
});
