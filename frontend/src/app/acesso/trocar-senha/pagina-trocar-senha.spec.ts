import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../conta-de-teste';
import { Sessao } from '../sessao';
import { PaginaTrocarSenha } from './pagina-trocar-senha';

describe('PaginaTrocarSenha', () => {
  let controle: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PaginaTrocarSenha],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    TestBed.inject(Sessao).definir(contaDeTeste({ trocaSenhaObrigatoria: true }));
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => controle.verify());

  async function renderizar(atual: string, nova: string, confirmacao: string) {
    const fixture = TestBed.createComponent(PaginaTrocarSenha);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    const valores: Record<string, string> = { senhaAtual: atual, novaSenha: nova, confirmacao };
    for (const [nome, valor] of Object.entries(valores)) {
      const campo = elemento.querySelector<HTMLInputElement>(`input[formcontrolname="${nome}"]`)!;
      campo.value = valor;
      campo.dispatchEvent(new Event('input'));
      campo.dispatchEvent(new Event('blur'));
    }
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    return elemento;
  }

  it('explica que a senha é temporária quando a troca é obrigatória', async () => {
    const fixture = TestBed.createComponent(PaginaTrocarSenha);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain(
      'Crie a sua senha',
    );
  });

  it('troca a senha e libera o sistema', async () => {
    await renderizar('temporaria', 'uma frase longa de senha', 'uma frase longa de senha');

    const pedido = controle.expectOne('/api/sessao/senha');
    expect(pedido.request.body).toEqual({
      senhaAtual: 'temporaria',
      novaSenha: 'uma frase longa de senha',
    });
    pedido.flush(contaDeTeste());

    expect(TestBed.inject(Sessao).conta()?.trocaSenhaObrigatoria).toBe(false);
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('não envia senha curta nem confirmação diferente', async () => {
    const elemento = await renderizar('temporaria', 'curta', 'curta');
    controle.expectNone('/api/sessao/senha');
    expect(elemento.textContent).toContain('Use pelo menos 12 caracteres.');

    const outro = await renderizar('temporaria', 'uma frase longa de senha', 'outra frase longa');
    controle.expectNone('/api/sessao/senha');
    expect(outro.textContent).toContain('As duas senhas novas não são iguais.');
  });
});
