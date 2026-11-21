import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../conta-de-teste';
import { Sessao } from '../sessao';
import { MenuConta } from './menu-conta';

describe('MenuConta', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MenuConta],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    TestBed.inject(Sessao).definir(contaDeTeste());
  });

  async function abrir() {
    const fixture = TestBed.createComponent(MenuConta);
    await fixture.whenStable();
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('[aria-label="Minha conta"]')!
      .click();
    await fixture.whenStable();
    return fixture;
  }

  it('mostra quem está logado e o perfil', async () => {
    await abrir();
    const painel = document.querySelector('.mat-mdc-menu-panel');

    expect(painel?.textContent).toContain('Pessoa Fictícia');
    expect(painel?.textContent).toContain('Coordenador');
    expect(painel?.textContent).toContain('Trocar senha');
  });

  it('sai, limpa a sessão e volta para a tela de entrada', async () => {
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    await abrir();

    const sair = Array.from(document.querySelectorAll<HTMLButtonElement>('[mat-menu-item]')).find(
      (item) => item.textContent?.includes('Sair'),
    );
    sair!.click();
    TestBed.inject(HttpTestingController).expectOne('/api/sessao/sair').flush(null);

    expect(TestBed.inject(Sessao).autenticada()).toBe(false);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/entrar');
  });
});
