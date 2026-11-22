import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';

import { Vinculo } from './colaboradores-api';
import { HistoricoVinculos } from './historico-vinculos';

interface Exposto {
  formulario: { patchValue(v: object): void };
  abrirRegistro(): void;
  salvar(): void;
}

const ENCERRADO: Vinculo = {
  id: 'v1',
  dataContratacao: '2024-02-01',
  dataDesligamento: '2025-03-31',
  motivoDesligamento: 'Fim do contrato.',
};

const ABERTO: Vinculo = {
  id: 'v2',
  dataContratacao: '2025-06-01',
  dataDesligamento: null,
  motivoDesligamento: null,
};

describe('HistoricoVinculos', () => {
  let controle: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HistoricoVinculos],
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

  async function criar(vinculos: Vinculo[], podeGerenciar = true) {
    const fixture = TestBed.createComponent(HistoricoVinculos);
    fixture.componentRef.setInput('colaboradorId', 'c1');
    fixture.componentRef.setInput('podeGerenciar', podeGerenciar);
    fixture.detectChanges();
    controle.expectOne('/api/colaboradores/c1/vinculos').flush(vinculos);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    return { fixture, elemento, componente: fixture.componentInstance as unknown as Exposto };
  }

  it('mostra o histórico com o vínculo atual e o motivo do desligamento', async () => {
    const { elemento } = await criar([ABERTO, ENCERRADO]);

    const itens = Array.from(elemento.querySelectorAll('li')).map((li) => li.textContent ?? '');
    expect(itens[0]).toContain('01/06/2025');
    expect(itens[0]).toContain('vínculo atual');
    expect(itens[1]).toContain('desligado em 31/03/2025');
    expect(itens[1]).toContain('Fim do contrato.');
    expect(elemento.textContent).toContain('Registrar desligamento');
  });

  it('registra o desligamento do vínculo aberto e avisa a ficha', async () => {
    const { fixture, componente } = await criar([ABERTO]);
    const mudou = vi.fn();
    fixture.componentInstance.mudou.subscribe(mudou);

    componente.abrirRegistro();
    componente.formulario.patchValue({ data: new Date(2026, 1, 10), motivo: '' });
    componente.salvar();
    controle.expectNone('/api/colaboradores/c1/desligamento');

    componente.formulario.patchValue({ motivo: '  Pedido de demissão. ' });
    componente.salvar();
    const pedido = controle.expectOne('/api/colaboradores/c1/desligamento');
    expect(pedido.request.body).toEqual({ data: '2026-02-10', motivo: 'Pedido de demissão.' });
    pedido.flush({ ...ABERTO, dataDesligamento: '2026-02-10', motivoDesligamento: 'x' });

    controle.expectOne('/api/colaboradores/c1/vinculos').flush([]);
    expect(mudou).toHaveBeenCalled();
  });

  it('registra a contratação quando não há vínculo aberto', async () => {
    const { componente } = await criar([ENCERRADO]);

    componente.abrirRegistro();
    componente.formulario.patchValue({ data: new Date(2026, 4, 2) });
    componente.salvar();

    const pedido = controle.expectOne('/api/colaboradores/c1/contratacoes');
    expect(pedido.request.body).toEqual({ dataContratacao: '2026-05-02' });
    pedido.flush(ABERTO);
    controle.expectOne('/api/colaboradores/c1/vinculos').flush([ABERTO, ENCERRADO]);
  });

  it('só mostra o histórico para quem não gerencia', async () => {
    const { elemento } = await criar([], false);

    expect(elemento.textContent).toContain('Nenhuma contratação registrada.');
    expect(elemento.querySelector('button')).toBeNull();
  });
});
