import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { routes } from './app.routes';

describe('Rotas', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes)],
    });
  });

  it('carrega a página inicial na raiz', async () => {
    const harness = await RouterTestingHarness.create('/');

    expect(harness.routeNativeElement?.textContent).toContain(
      'Boas-vindas ao Sistema de Gestão do Centro-Dia',
    );
  });

  it('mostra a página 404 para endereço inexistente', async () => {
    const harness = await RouterTestingHarness.create('/nao-existe');

    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });
});
