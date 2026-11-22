import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { FormularioParceiro } from './formulario-parceiro';
import { parceiroFicticio } from './parceiro-ficticio';

interface Exposto {
  formulario: { patchValue(v: object): void; disabled: boolean; hasError(e: string): boolean };
  salvar(): void;
}

describe('FormularioParceiro', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioParceiro],
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
      contaDeTeste({ permissoes: ['PARCERIAS_LER', 'PARCERIAS_GERENCIAR'] }),
    );
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => controle.verify());

  it('cadastra o parceiro com vigência e campos opcionais vazios como nulos', async () => {
    const fixture = TestBed.createComponent(FormularioParceiro);
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as Exposto;
    componente.formulario.patchValue({
      nome: ' CRAS Fictício ',
      tipo: 'ORGAO_PUBLICO',
      instrumento: 'Termo 1/2026',
      vigenciaInicio: new Date(2026, 0, 1),
      vigenciaFim: new Date(2026, 11, 31),
      contato: '  ',
    });
    componente.salvar();

    const pedido = controle.expectOne('/api/parceiros');
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body).toEqual({
      nome: 'CRAS Fictício',
      tipo: 'ORGAO_PUBLICO',
      instrumento: 'Termo 1/2026',
      vigenciaInicio: '2026-01-01',
      vigenciaFim: '2026-12-31',
      contato: null,
      observacoes: null,
    });
    pedido.flush(parceiroFicticio({ id: 'p9' }));

    expect(navegar).toHaveBeenCalledWith(['/parcerias', 'p9']);
  });

  it('não envia com a vigência invertida', async () => {
    const fixture = TestBed.createComponent(FormularioParceiro);
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as Exposto;
    componente.formulario.patchValue({
      nome: 'Fictício',
      tipo: 'OUTRO',
      vigenciaInicio: new Date(2026, 5, 1),
      vigenciaFim: new Date(2026, 4, 1),
    });
    componente.salvar();

    expect(componente.formulario.hasError('vigencia')).toBe(true);
    controle.expectNone('/api/parceiros');
  });

  it('edita com a situação e carrega as ações de articulação', async () => {
    const fixture = TestBed.createComponent(FormularioParceiro);
    fixture.componentRef.setInput('id', 'p1');
    await fixture.whenStable();
    controle.expectOne('/api/parceiros/p1').flush(parceiroFicticio({ id: 'p1' }));
    await fixture.whenStable();
    controle.expectOne('/api/parceiros/p1/articulacoes').flush([]);

    const componente = fixture.componentInstance as unknown as Exposto;
    componente.formulario.patchValue({ ativo: false });
    componente.salvar();
    const pedido = controle.expectOne('/api/parceiros/p1');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body).toEqual(expect.objectContaining({ ativo: false }));
    pedido.flush(parceiroFicticio({ id: 'p1', ativo: false }));
    await fixture.whenStable();

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.situacao')?.textContent,
    ).toContain('Inativo');
  });

  it('só mostra os dados para quem não gerencia', async () => {
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes: ['PARCERIAS_LER'] }));
    const fixture = TestBed.createComponent(FormularioParceiro);
    fixture.componentRef.setInput('id', 'p1');
    await fixture.whenStable();
    controle.expectOne('/api/parceiros/p1').flush(parceiroFicticio({ id: 'p1' }));
    await fixture.whenStable();
    controle.expectOne('/api/parceiros/p1/articulacoes').flush([]);
    await fixture.whenStable();

    expect((fixture.componentInstance as unknown as Exposto).formulario.disabled).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Salvar parceiro');
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Registrar ação');
  });
});
