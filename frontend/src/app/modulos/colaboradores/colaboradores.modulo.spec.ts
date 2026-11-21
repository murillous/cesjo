import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
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

describe('Módulo de colaboradores', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(),
        provideHttpClientTesting(),
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

  it('aparece no menu para quem tem COLABORADORES_LER', () => {
    entrarCom(['COLABORADORES_LER']);

    expect(itensDoMenu()).toContain('/colaboradores');
  });

  it('some do menu para quem não tem COLABORADORES_LER', () => {
    entrarCom(['NOTIFICACOES_LER']);

    expect(itensDoMenu()).not.toContain('/colaboradores');
  });

  it('não abre a rota para quem não tem COLABORADORES_LER', async () => {
    entrarCom(['NOTIFICACOES_LER']);

    const harness = await RouterTestingHarness.create('/colaboradores');

    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });

  it('não abre o cadastro para quem só lê', async () => {
    entrarCom(['COLABORADORES_LER']);

    const harness = await RouterTestingHarness.create('/colaboradores/novo');

    expect(harness.routeNativeElement?.textContent).not.toContain('Cadastrar colaborador');
  });
});
