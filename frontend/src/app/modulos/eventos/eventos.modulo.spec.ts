import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { routes } from '../../app.routes';
import { Menu } from '../../layout/menu/menu';

function entrarCom(permissoes: readonly Permissao[]): void {
  TestBed.inject(Sessao).definir(contaDeTeste({ permissoes }));
}

describe('Módulo de reuniões e eventos', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
  });

  function itensDoMenu(): string[] {
    const fixture = TestBed.createComponent(Menu);
    fixture.detectChanges();
    const elemento = fixture.nativeElement as HTMLElement;
    return Array.from(elemento.querySelectorAll('a')).map((a) => a.getAttribute('href') ?? '');
  }

  it('aparece no menu só para quem tem EVENTOS_LER', () => {
    entrarCom(['EVENTOS_LER']);
    expect(itensDoMenu()).toContain('/eventos');
  });

  it('some do menu e não abre a rota sem EVENTOS_LER', async () => {
    entrarCom(['NOTIFICACOES_LER']);
    expect(itensDoMenu()).not.toContain('/eventos');

    const harness = await RouterTestingHarness.create('/eventos');
    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });

  it('não abre o cadastro para quem só lê', async () => {
    entrarCom(['EVENTOS_LER']);

    const harness = await RouterTestingHarness.create('/eventos/novo');

    expect(harness.routeNativeElement?.querySelector('button[type="submit"]')).toBeNull();
  });
});
