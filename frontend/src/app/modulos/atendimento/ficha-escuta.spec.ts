import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { escutaFicticia } from './escuta-ficticia';
import { FichaEscuta } from './ficha-escuta';

interface FichaExposta {
  formulario: { patchValue(v: object): void };
  corrigir(): void;
  salvar(): void;
}

describe('FichaEscuta', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FichaEscuta],
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

  function responderProfissionais(sugeridoId: string | null = null): void {
    controle.expectOne('/api/escutas/profissionais').flush({
      profissionais: [{ id: 'c1', nome: 'Psicóloga Fictícia', funcao: 'PSICOLOGO' }],
      sugeridoId,
    });
  }

  it('registra a escuta com o profissional sugerido pela conta', async () => {
    const fixture = TestBed.createComponent(FichaEscuta);
    fixture.componentRef.setInput('pessoaId', 'p1');
    await fixture.whenStable();
    controle.expectOne('/api/atendimento/pessoas/p1').flush({ id: 'p1', nomeCompleto: 'Pessoa' });
    responderProfissionais('c1');
    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.formulario.patchValue({
      dataEscuta: new Date(2026, 10, 20),
      potencialidades: '  Comunicativa (fictício) ',
    });
    ficha.salvar();

    const pedido = controle.expectOne('/api/escutas');
    expect(pedido.request.body).toEqual({
      pessoaId: 'p1',
      dados: {
        dataEscuta: '2026-11-20',
        profissionalId: 'c1',
        dinamicaFamiliar: null,
        necessidadesImediatas: null,
        habilidades: null,
        potencialidades: 'Comunicativa (fictício)',
      },
    });
    pedido.flush(escutaFicticia());

    expect(navegar).toHaveBeenCalledWith(['/atendimento', 'pessoas', 'p1']);
  });

  it('não envia escuta sem nenhum texto', async () => {
    const fixture = TestBed.createComponent(FichaEscuta);
    fixture.componentRef.setInput('pessoaId', 'p1');
    await fixture.whenStable();
    controle.expectOne('/api/atendimento/pessoas/p1').flush({ id: 'p1', nomeCompleto: 'Pessoa' });
    responderProfissionais('c1');
    (fixture.componentInstance as unknown as FichaExposta).salvar();
    await fixture.whenStable();

    controle.expectNone('/api/escutas');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Preencha pelo menos um dos campos da escuta.',
    );
  });

  it('mostra a escuta e corrige', async () => {
    const fixture = TestBed.createComponent(FichaEscuta);
    fixture.componentRef.setInput('id', 'e1');
    await fixture.whenStable();
    responderProfissionais();
    controle.expectOne('/api/escutas/e1').flush(escutaFicticia());
    await fixture.whenStable();
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Texto fictício de dinâmica familiar.');
    expect(texto).toContain('Não registrado.');

    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.corrigir();
    ficha.formulario.patchValue({ habilidades: 'Desenha bem' });
    ficha.salvar();
    const pedido = controle.expectOne('/api/escutas/e1');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body.habilidades).toBe('Desenha bem');
    pedido.flush(escutaFicticia({ habilidades: 'Desenha bem' }));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Desenha bem');
  });

  it('esconde a correção de quem só lê', async () => {
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes: ['ESCUTA_LER'] }));
    const fixture = TestBed.createComponent(FichaEscuta);
    fixture.componentRef.setInput('id', 'e1');
    await fixture.whenStable();
    controle.expectOne('/api/escutas/e1').flush(escutaFicticia());
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Corrigir escuta');
  });
});
