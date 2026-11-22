import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { doacaoFicticia } from './doacao-ficticia';
import { FormularioDoacao } from './formulario-doacao';

interface Exposto {
  formulario: { patchValue(v: object): void; disabled: boolean };
  salvar(): void;
}

describe('FormularioDoacao', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioDoacao],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => controle.verify());

  async function criar(permissoes = ['DOACOES_LER', 'DOACOES_REGISTRAR'] as const) {
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes: [...permissoes] }));
    const fixture = TestBed.createComponent(FormularioDoacao);
    await fixture.whenStable();
    return { fixture, componente: fixture.componentInstance as unknown as Exposto };
  }

  it('registra a doação em dinheiro com o valor digitado com vírgula', async () => {
    const { componente } = await criar();
    componente.formulario.patchValue({
      direcao: 'RECEBIDA',
      data: new Date(2026, 4, 4),
      contraparte: ' Mercado Fictício ',
      tipo: 'DINHEIRO',
      descricao: 'Festa junina.',
      quantidade: 'ignorada',
      valor: '150,5',
    });
    componente.salvar();

    const pedido = controle.expectOne('/api/doacoes');
    expect(pedido.request.body).toEqual({
      direcao: 'RECEBIDA',
      data: '2026-05-04',
      contraparte: 'Mercado Fictício',
      tipo: 'DINHEIRO',
      descricao: 'Festa junina.',
      quantidade: null,
      valor: 150.5,
    });
    pedido.flush(doacaoFicticia({ id: 'd9' }));

    expect(navegar).toHaveBeenCalledWith(['/doacoes', 'd9']);
  });

  it('não manda valor fora da doação em dinheiro e recusa valor zero ou mal escrito', async () => {
    const { componente } = await criar();
    componente.formulario.patchValue({
      direcao: 'REALIZADA',
      data: new Date(2026, 4, 4),
      contraparte: 'Família fictícia',
      tipo: 'DINHEIRO',
      descricao: 'Ajuda.',
      valor: '0',
    });
    componente.salvar();
    componente.formulario.patchValue({ valor: '10,999' });
    componente.salvar();
    controle.expectNone('/api/doacoes');

    componente.formulario.patchValue({ tipo: 'ALIMENTOS', valor: '' });
    componente.salvar();
    expect(controle.expectOne('/api/doacoes').request.body).toEqual(
      expect.objectContaining({ tipo: 'ALIMENTOS', valor: null }),
    );
  });

  it('só mostra os dados para quem não registra', async () => {
    const { fixture, componente } = await criar(['DOACOES_LER'] as unknown as [
      'DOACOES_LER',
      'DOACOES_REGISTRAR',
    ]);
    fixture.componentRef.setInput('id', 'd1');
    await fixture.whenStable();
    controle
      .expectOne('/api/doacoes/d1')
      .flush(doacaoFicticia({ id: 'd1', tipo: 'DINHEIRO', quantidade: null, valor: 20 }));
    await fixture.whenStable();

    expect(componente.formulario.disabled).toBe(true);
    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.querySelector('button[type="submit"]')).toBeNull();
    expect((elemento.querySelector('input[inputmode="decimal"]') as HTMLInputElement).value).toBe(
      '20,00',
    );
  });
});
