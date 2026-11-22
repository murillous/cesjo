import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { comunicacaoFicticia } from './comunicacao-ficticia';
import { FichaComunicacao } from './ficha-comunicacao';

interface FichaExposta {
  formulario: { patchValue(v: object): void; invalid: boolean };
  rotuloContraparte(): string;
  corrigir(): void;
  salvar(): void;
}

describe('FichaComunicacao', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FichaComunicacao],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    TestBed.inject(Sessao).definir(
      contaDeTeste({ permissoes: ['COMUNICACOES_LER', 'COMUNICACOES_REGISTRAR'] }),
    );
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => controle.verify());

  it('registra o contato sem descrição e abre a ficha', async () => {
    const fixture = TestBed.createComponent(FichaComunicacao);
    await fixture.whenStable();
    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.formulario.patchValue({
      tipo: 'CONTATO_INSTITUCIONAL',
      data: new Date(2026, 10, 20),
      contraparte: ' CRAS Fictício ',
      assunto: ' Vaga para usuário ',
      descricao: '  ',
    });
    expect(ficha.rotuloContraparte()).toBe('Instituição');
    ficha.salvar();

    const pedido = controle.expectOne('/api/comunicacoes');
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body).toEqual({
      tipo: 'CONTATO_INSTITUCIONAL',
      data: '2026-11-20',
      contraparte: 'CRAS Fictício',
      assunto: 'Vaga para usuário',
      descricao: null,
    });
    pedido.flush(comunicacaoFicticia());

    expect(navegar).toHaveBeenCalledWith(['/comunicacoes', 'c1']);
  });

  it('não envia sem tipo nem assunto', async () => {
    const fixture = TestBed.createComponent(FichaComunicacao);
    await fixture.whenStable();
    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.formulario.patchValue({ contraparte: 'Pessoa Fictícia' });
    ficha.salvar();
    fixture.detectChanges();

    expect(ficha.formulario.invalid).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Escolha o tipo.');
    controle.expectNone('/api/comunicacoes');
  });

  it('mostra a ficha e corrige o registro', async () => {
    const fixture = TestBed.createComponent(FichaComunicacao);
    fixture.componentRef.setInput('id', 'c1');
    await fixture.whenStable();
    controle.expectOne('/api/comunicacoes/c1').flush(comunicacaoFicticia());
    await fixture.whenStable();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Vaga para usuário');
    expect(texto).toContain('CRAS Fictício');
    expect(texto).toContain('Retorno combinado (fictício).');

    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.corrigir();
    ficha.formulario.patchValue({ assunto: 'Vaga confirmada' });
    ficha.salvar();
    const pedido = controle.expectOne('/api/comunicacoes/c1');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body.assunto).toBe('Vaga confirmada');
    pedido.flush(comunicacaoFicticia({ assunto: 'Vaga confirmada' }));
    expect(navegar).not.toHaveBeenCalled();
  });
});
