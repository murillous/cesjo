import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { FichaPia } from './ficha-pia';
import { piaFicticio } from './pia-ficticio';

interface FichaExposta {
  formulario: {
    patchValue(v: object): void;
    controls: { objetivos: { at(i: number): { patchValue(v: object): void }; length: number } };
  };
  acrescentarObjetivo(): void;
  removerObjetivo(i: number): void;
  editar(): void;
  salvar(): void;
}

describe('FichaPia', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FichaPia],
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

  async function abrir(id: string, pia = piaFicticio({ id })): Promise<ComponentFixture<FichaPia>> {
    const fixture = TestBed.createComponent(FichaPia);
    fixture.componentRef.setInput('id', id);
    await fixture.whenStable();
    controle.expectOne(`/api/pias/${id}`).flush(pia);
    await fixture.whenStable();
    return fixture;
  }

  it('elabora o PIA com objetivos, metas, prazo e responsável', async () => {
    const fixture = TestBed.createComponent(FichaPia);
    fixture.componentRef.setInput('pessoaId', 'p1');
    await fixture.whenStable();
    controle.expectOne('/api/atendimento/pessoas/p1').flush({ id: 'p1', nomeCompleto: 'Pessoa' });
    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.formulario.patchValue({ diagnostico: ' Diagnóstico ' });
    ficha.formulario.controls.objetivos.at(0).patchValue({
      descricao: 'Ir sozinha à padaria',
      prazo: new Date(2027, 2, 31),
    });
    ficha.acrescentarObjetivo();
    ficha.formulario.controls.objetivos
      .at(1)
      .patchValue({ descricao: 'Segundo', responsavel: 'Mãe' });
    ficha.salvar();

    const pedido = controle.expectOne('/api/pias');
    expect(pedido.request.body).toEqual({
      pessoaId: 'p1',
      conteudo: {
        diagnostico: 'Diagnóstico',
        objetivos: [
          {
            descricao: 'Ir sozinha à padaria',
            metas: null,
            acoes: null,
            prazo: '2027-03-31',
            responsavel: null,
          },
          { descricao: 'Segundo', metas: null, acoes: null, prazo: null, responsavel: 'Mãe' },
        ],
        observacoes: null,
      },
    });
    pedido.flush(piaFicticio({ id: 'pia9' }));

    expect(navegar).toHaveBeenCalledWith(['/atendimento', 'pias', 'pia9']);
  });

  it('não envia objetivo sem descrição', async () => {
    const fixture = TestBed.createComponent(FichaPia);
    fixture.componentRef.setInput('pessoaId', 'p1');
    await fixture.whenStable();
    controle.expectOne('/api/atendimento/pessoas/p1').flush({ id: 'p1', nomeCompleto: 'Pessoa' });
    (fixture.componentInstance as unknown as FichaExposta).salvar();

    controle.expectNone('/api/pias');
  });

  it('mostra o plano e edita a versão em elaboração', async () => {
    const fixture = await abrir('pia1');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Ampliar a autonomia');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('31/03/2027');

    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.editar();
    ficha.formulario.patchValue({ observacoes: 'Família participou' });
    ficha.salvar();
    const pedido = controle.expectOne('/api/pias/pia1');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body.observacoes).toBe('Família participou');
    expect(pedido.request.body.objetivos[0].prazo).toBe('2027-03-31');
    pedido.flush(
      piaFicticio({ conteudo: { ...piaFicticio().conteudo, observacoes: 'Família participou' } }),
    );
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Família participou');
  });

  it('não oferece editar versão que não está em elaboração', async () => {
    const fixture = await abrir(
      'pia2',
      piaFicticio({ id: 'pia2', editavel: false, status: 'APROVADO' }),
    );

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Editar PIA');
  });
});
