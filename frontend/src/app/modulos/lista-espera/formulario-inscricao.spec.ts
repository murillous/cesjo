import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
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
      laudoMedicoArquivoId: 'arquivo-laudo',
      comprovanteResidenciaArquivoId: null,
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
      laudoMedicoArquivoId: 'arquivo-laudo',
      comprovanteResidenciaArquivoId: null,
    });
    pedido.flush({ id: 'i1', nomeCompleto: 'Pessoa Fictícia', cadastroCompleto: false });

    expect(navegar).toHaveBeenCalledWith(['/lista-espera']);
  });

  it('mostra os campos de documento só para quem envia documentos', async () => {
    const sessao = TestBed.inject(Sessao);
    sessao.definir(contaDeTeste({ permissoes: ['LISTA_ESPERA_CADASTRAR'] }));
    let fixture = TestBed.createComponent(FormularioInscricao);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('app-envio-arquivo')).toBeNull();

    sessao.definir(contaDeTeste());
    fixture = TestBed.createComponent(FormularioInscricao);
    await fixture.whenStable();
    const campos = (fixture.nativeElement as HTMLElement).querySelectorAll('app-envio-arquivo');
    expect(campos.length).toBe(2);
    expect(campos[0].textContent).toContain('Laudo médico');
    expect(campos[1].textContent).toContain('Comprovante de residência');
  });

  it('na edição, carrega o candidato, envia PUT sem anexos e volta para a ficha', async () => {
    TestBed.inject(Sessao).definir(contaDeTeste());
    const fixture = TestBed.createComponent(FormularioInscricao);
    fixture.componentRef.setInput('id', 'i1');
    fixture.detectChanges();
    controle.expectOne('/api/lista-espera/inscricoes/i1').flush({
      id: 'i1',
      nomeCompleto: 'Pessoa Fictícia',
      dataNascimento: null,
      tipoDeficiencia: 'FISICA',
      telefone: '3132345678',
      nomeResponsavel: 'Responsável Fictício',
      origemEncaminhamento: 'SAUDE',
      dataCadastro: '2026-11-02',
    });
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.querySelector('h1')?.textContent).toContain('Editar candidato');
    expect(elemento.querySelector('app-envio-arquivo')).toBeNull();

    (fixture.componentInstance as unknown as Componente).salvar();
    const pedido = controle.expectOne('/api/lista-espera/inscricoes/i1');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body).toEqual({
      nomeCompleto: 'Pessoa Fictícia',
      dataNascimento: null,
      tipoDeficiencia: 'FISICA',
      telefone: '3132345678',
      nomeResponsavel: 'Responsável Fictício',
      origemEncaminhamento: 'SAUDE',
      dataCadastro: '2026-11-02',
    });
    pedido.flush({ id: 'i1' });
    expect(navegar).toHaveBeenCalledWith(['/lista-espera', 'i1']);
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
      laudoMedicoArquivoId: null,
      comprovanteResidenciaArquivoId: null,
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
