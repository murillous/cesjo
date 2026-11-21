import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../conta-de-teste';
import { Sessao } from '../sessao';
import { PaginaEntrar } from './pagina-entrar';

describe('PaginaEntrar', () => {
  let controle: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PaginaEntrar],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => controle.verify());

  async function renderizar(retorno?: string) {
    const fixture = TestBed.createComponent(PaginaEntrar);
    if (retorno) {
      fixture.componentRef.setInput('retorno', retorno);
    }
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    const preencher = (seletor: string, valor: string) => {
      const campo = elemento.querySelector<HTMLInputElement>(seletor)!;
      campo.value = valor;
      campo.dispatchEvent(new Event('input'));
    };
    preencher('input[formcontrolname="login"]', 'coordenacao');
    preencher('input[formcontrolname="senha"]', 'senha-de-teste');
    return { fixture, elemento };
  }

  function enviar(elemento: HTMLElement) {
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    controle.expectOne('/api/sessao/csrf').flush({});
    return controle.expectOne('/api/sessao/entrar');
  }

  it('entra, guarda a sessão e volta para o destino pedido', async () => {
    const { elemento } = await renderizar('/contas');

    const pedido = enviar(elemento);
    expect(pedido.request.body).toEqual({ login: 'coordenacao', senha: 'senha-de-teste' });
    pedido.flush(contaDeTeste());

    expect(TestBed.inject(Sessao).conta()?.login).toBe('coordenacao');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/contas');
  });

  it('não aceita destino fora do sistema', async () => {
    const { elemento } = await renderizar('//exemplo.invalido');

    enviar(elemento).flush(contaDeTeste());

    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('leva para a troca de senha no primeiro acesso', async () => {
    const { elemento } = await renderizar();

    enviar(elemento).flush(contaDeTeste({ trocaSenhaObrigatoria: true }));

    expect(router.navigateByUrl).toHaveBeenCalledWith('/trocar-senha');
  });

  it('mostra a mensagem da API quando o login falha', async () => {
    const { fixture, elemento } = await renderizar();

    enviar(elemento).flush(
      { detail: 'Login ou senha incorretos.' },
      { status: 401, statusText: 'Unauthorized' },
    );
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Login ou senha incorretos.',
    );
    expect(TestBed.inject(Sessao).autenticada()).toBe(false);
  });
});
