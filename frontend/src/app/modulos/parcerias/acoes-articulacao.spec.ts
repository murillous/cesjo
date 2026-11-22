import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';

import { AcoesArticulacao } from './acoes-articulacao';
import { Articulacao } from './parcerias-api';

interface Exposto {
  formulario: { patchValue(v: object): void };
  abrirRegistro(): void;
  salvar(): void;
}

const ACAO: Articulacao = {
  id: 'a1',
  data: '2026-03-10',
  descricao: 'Reunião para estágio supervisionado.',
  criadoEm: '2026-03-10T12:00:00Z',
};

describe('AcoesArticulacao', () => {
  let controle: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AcoesArticulacao],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controle.verify());

  async function criar(acoes: Articulacao[]) {
    const fixture = TestBed.createComponent(AcoesArticulacao);
    fixture.componentRef.setInput('parceiroId', 'p1');
    fixture.componentRef.setInput('podeGerenciar', true);
    fixture.detectChanges();
    controle.expectOne('/api/parceiros/p1/articulacoes').flush(acoes);
    await fixture.whenStable();
    return { fixture, componente: fixture.componentInstance as unknown as Exposto };
  }

  it('lista as ações com data e descrição', async () => {
    const { fixture } = await criar([ACAO]);

    const item = (fixture.nativeElement as HTMLElement).querySelector('li')?.textContent ?? '';
    expect(item).toContain('10/03/2026');
    expect(item).toContain('Reunião para estágio supervisionado.');
  });

  it('registra uma ação e recarrega a lista', async () => {
    const { componente } = await criar([]);

    componente.abrirRegistro();
    componente.formulario.patchValue({ data: new Date(2026, 4, 2), descricao: ' ' });
    componente.salvar();
    controle.expectNone((r) => r.method === 'POST');

    componente.formulario.patchValue({ descricao: ' Assinatura do termo. ' });
    componente.salvar();
    const pedido = controle.expectOne(
      (r) => r.method === 'POST' && r.url === '/api/parceiros/p1/articulacoes',
    );
    expect(pedido.request.body).toEqual({ data: '2026-05-02', descricao: 'Assinatura do termo.' });
    pedido.flush(ACAO);
    controle.expectOne('/api/parceiros/p1/articulacoes').flush([ACAO]);
  });
});
