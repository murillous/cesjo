import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { contaFicticia } from '../contas/conta-ficticia';
import { colaboradorFicticio } from './colaborador-ficticio';
import { FormularioColaborador } from './formulario-colaborador';

interface FormularioExposto {
  formulario: { setValue(v: object): void; patchValue(v: object): void; disabled: boolean };
  salvar(): void;
}

describe('FormularioColaborador', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioColaborador],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    TestBed.inject(Sessao).definir(contaDeTeste());
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => controle.verify());

  function responderContas(): void {
    controle
      .expectOne((r) => r.url === '/api/contas')
      .flush({
        content: [contaFicticia()],
        page: { size: 200, number: 0, totalElements: 1, totalPages: 1 },
      });
  }

  it('cadastra o colaborador com a conta escolhida', async () => {
    const fixture = TestBed.createComponent(FormularioColaborador);
    responderContas();
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as FormularioExposto;
    componente.formulario.patchValue({
      nome: 'Nova Colaboradora',
      funcao: 'TERAPEUTA_OCUPACIONAL',
      contaId: contaFicticia().id,
    });
    componente.salvar();

    const pedido = controle.expectOne('/api/colaboradores');
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body).toEqual({
      nome: 'Nova Colaboradora',
      funcao: 'TERAPEUTA_OCUPACIONAL',
      contaId: contaFicticia().id,
      dataContratacao: null,
    });
    pedido.flush(colaboradorFicticio({ id: 'c9' }));

    expect(navegar).toHaveBeenCalledWith(['/colaboradores', 'c9']);
  });

  it('cadastra com a data de contratação, que abre o primeiro vínculo', async () => {
    const fixture = TestBed.createComponent(FormularioColaborador);
    responderContas();
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as FormularioExposto;
    componente.formulario.patchValue({
      nome: 'Nova Colaboradora',
      funcao: 'OUTRA',
      dataContratacao: new Date(2026, 0, 5),
    });
    componente.salvar();

    const pedido = controle.expectOne('/api/colaboradores');
    expect(pedido.request.body).toEqual(
      expect.objectContaining({ dataContratacao: '2026-01-05', contaId: null }),
    );
    pedido.flush(colaboradorFicticio({ id: 'c9' }));
  });

  it('não envia o formulário incompleto', async () => {
    const fixture = TestBed.createComponent(FormularioColaborador);
    responderContas();
    await fixture.whenStable();
    (fixture.componentInstance as unknown as FormularioExposto).salvar();

    controle.expectNone('/api/colaboradores');
  });

  it('inativa o colaborador e desliga a conta', async () => {
    const fixture = TestBed.createComponent(FormularioColaborador);
    fixture.componentRef.setInput('id', 'c1');
    responderContas();
    await fixture.whenStable();
    const ligada = { id: 'a9', login: 'colab.antigo', nome: 'Colab Antigo', ativa: false };
    controle
      .expectOne('/api/colaboradores/c1')
      .flush(colaboradorFicticio({ id: 'c1', conta: ligada }));
    await fixture.whenStable();
    controle.expectOne('/api/colaboradores/c1/vinculos').flush([]);

    const componente = fixture.componentInstance as unknown as FormularioExposto;
    componente.formulario.patchValue({ ativo: false, contaId: '' });
    componente.salvar();

    const pedido = controle.expectOne('/api/colaboradores/c1');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body).toEqual({
      nome: 'Colaboradora Fictícia',
      funcao: 'ASSISTENTE_SOCIAL',
      contaId: null,
      ativo: false,
    });
    pedido.flush(colaboradorFicticio({ id: 'c1', ativo: false }));
    await fixture.whenStable();

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.situacao')?.textContent,
    ).toContain('Inativo');
  });

  it('mostra o erro da API', async () => {
    const fixture = TestBed.createComponent(FormularioColaborador);
    responderContas();
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as FormularioExposto;
    componente.formulario.patchValue({ nome: 'Fictícia', funcao: 'OUTRA', contaId: 'a1' });
    componente.salvar();
    controle
      .expectOne('/api/colaboradores')
      .flush(
        { detail: 'Esta conta de acesso já está ligada a outro colaborador.' },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await fixture.whenStable();

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent,
    ).toContain('Esta conta de acesso já está ligada a outro colaborador.');
  });

  it('só mostra os dados para quem não gerencia', async () => {
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes: ['COLABORADORES_LER'] }));
    const fixture = TestBed.createComponent(FormularioColaborador);
    fixture.componentRef.setInput('id', 'c1');
    await fixture.whenStable();
    controle.expectOne('/api/colaboradores/c1').flush(colaboradorFicticio({ id: 'c1' }));
    await fixture.whenStable();
    controle.expectOne('/api/colaboradores/c1/vinculos').flush([]);
    await fixture.whenStable();

    expect((fixture.componentInstance as unknown as FormularioExposto).formulario.disabled).toBe(
      true,
    );
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Salvar colaborador');
  });
});
