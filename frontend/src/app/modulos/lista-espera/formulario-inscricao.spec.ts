import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { FormularioInscricao } from './formulario-inscricao';

interface Componente {
  formulario: {
    setValue(v: object): void;
    controls: Record<string, { hasError(codigo: string): boolean; setValue(v: unknown): void }>;
    valid: boolean;
  };
  salvar(): void;
}

describe('FormularioInscricao', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioInscricao],
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

  async function criar(): Promise<Componente> {
    const fixture = TestBed.createComponent(FormularioInscricao);
    await fixture.whenStable();
    return fixture.componentInstance as unknown as Componente;
  }

  it('cadastra o candidato com as datas em AAAA-MM-DD e volta para a lista', async () => {
    const componente = await criar();
    componente.formulario.setValue({
      nomeCompleto: ' Pessoa Fictícia ',
      dataNascimento: new Date(1990, 4, 10),
      tipoDeficiencia: 'TEA',
      telefone: '(31) 98765-4321',
      nomeResponsavel: 'Responsável Fictício',
      origemEncaminhamento: 'CRAS',
      dataCadastro: new Date(2026, 10, 2),
    });
    componente.salvar();

    const pedido = controle.expectOne('/api/lista-espera/inscricoes');
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body).toEqual({
      nomeCompleto: 'Pessoa Fictícia',
      dataNascimento: '1990-05-10',
      tipoDeficiencia: 'TEA',
      telefone: '(31) 98765-4321',
      nomeResponsavel: 'Responsável Fictício',
      origemEncaminhamento: 'CRAS',
      dataCadastro: '2026-11-02',
    });
    pedido.flush({ id: 'i1', nomeCompleto: 'Pessoa Fictícia' });

    expect(navegar).toHaveBeenCalledWith(['/lista-espera']);
  });

  it('não envia sem os campos obrigatórios', async () => {
    const componente = await criar();
    componente.salvar();

    controle.expectNone('/api/lista-espera/inscricoes');
    expect(componente.formulario.controls['nomeCompleto'].hasError('required')).toBe(true);
    expect(componente.formulario.controls['tipoDeficiencia'].hasError('required')).toBe(true);
    expect(componente.formulario.controls['dataNascimento'].hasError('required')).toBe(false);
  });

  it('recusa telefone sem DDD', async () => {
    const componente = await criar();
    componente.formulario.controls['telefone'].setValue('98765-4321');
    expect(componente.formulario.controls['telefone'].hasError('pattern')).toBe(true);
    componente.formulario.controls['telefone'].setValue('31987654321');
    expect(componente.formulario.controls['telefone'].hasError('pattern')).toBe(false);
  });

  it('mostra o erro do backend', async () => {
    const fixture = TestBed.createComponent(FormularioInscricao);
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as Componente;
    componente.formulario.setValue({
      nomeCompleto: 'Pessoa Fictícia',
      dataNascimento: null,
      tipoDeficiencia: 'FISICA',
      telefone: '3132345678',
      nomeResponsavel: 'Responsável Fictício',
      origemEncaminhamento: 'SAUDE',
      dataCadastro: new Date(2026, 10, 2),
    });
    componente.salvar();
    controle
      .expectOne('/api/lista-espera/inscricoes')
      .flush(
        { detail: 'Dados inválidos.', erros: [{ campo: 'telefone', mensagem: 'inválido' }] },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    const alerta = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alerta?.textContent).toContain('telefone: inválido');
    expect(navegar).not.toHaveBeenCalled();
  });
});
