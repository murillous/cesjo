import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { atividadeFicticia } from './atividade-ficticia';
import { FichaAtividade } from './ficha-atividade';

interface FichaExposta {
  formulario: { patchValue(v: object): void; invalid: boolean };
  corrigir(): void;
  salvar(): void;
}

describe('FichaAtividade', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FichaAtividade],
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
      contaDeTeste({ permissoes: ['ATIVIDADES_LER', 'ATIVIDADES_REGISTRAR'] }),
    );
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => controle.verify());

  function responderOpcoes(sugeridoId: string | null = null): void {
    controle.expectOne('/api/atividades/opcoes').flush({
      usuarios: [{ pessoaId: 'p1', nomeCompleto: 'Ana Fictícia' }],
      profissionais: [{ id: 'c1', nome: 'Terapeuta Fictícia', funcao: 'TERAPEUTA_OCUPACIONAL' }],
      sugeridoId,
    });
  }

  it('registra a atividade com o profissional sugerido pela conta', async () => {
    const fixture = TestBed.createComponent(FichaAtividade);
    await fixture.whenStable();
    responderOpcoes('c1');
    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.formulario.patchValue({
      tipo: 'COM_USUARIOS',
      data: new Date(2026, 10, 20),
      titulo: ' Oficina de música ',
      descricao: ' Roda de música (fictício). ',
      participantes: ['p1'],
    });
    ficha.salvar();

    const pedido = controle.expectOne('/api/atividades');
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body).toEqual({
      tipo: 'COM_USUARIOS',
      data: '2026-11-20',
      titulo: 'Oficina de música',
      descricao: 'Roda de música (fictício).',
      participantes: ['p1'],
      profissionais: ['c1'],
    });
    pedido.flush(atividadeFicticia());

    expect(navegar).toHaveBeenCalledWith(['/atividades', 'a1']);
  });

  it('não envia sem participante', async () => {
    const fixture = TestBed.createComponent(FichaAtividade);
    await fixture.whenStable();
    responderOpcoes('c1');
    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.formulario.patchValue({ tipo: 'COM_FAMILIAS', titulo: 'Reunião', descricao: 'Texto' });
    ficha.salvar();

    expect(ficha.formulario.invalid).toBe(true);
    controle.expectNone('/api/atividades');
  });

  it('não envia estudo de caso com mais de um usuário nem multiprofissional com um profissional', async () => {
    const fixture = TestBed.createComponent(FichaAtividade);
    await fixture.whenStable();
    responderOpcoes('c1');
    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.formulario.patchValue({
      tipo: 'ESTUDO_DE_CASO',
      titulo: 'Estudo',
      descricao: 'Texto (fictício)',
      participantes: ['p1', 'p2'],
    });
    ficha.salvar();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Atendimentos e estudos de caso são de um só usuário atendido.',
    );

    ficha.formulario.patchValue({ tipo: 'ATENDIMENTO_MULTIPROFISSIONAL', participantes: ['p1'] });
    ficha.salvar();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'precisa de pelo menos 2 profissionais',
    );
    controle.expectNone('/api/atividades');
  });

  it('avisa que o atendimento aberto é sigiloso', async () => {
    const fixture = TestBed.createComponent(FichaAtividade);
    fixture.componentRef.setInput('id', 'a1');
    await fixture.whenStable();
    controle
      .expectOne('/api/atividades/opcoes')
      .flush({ usuarios: [], profissionais: [], sugeridoId: null });
    controle
      .expectOne('/api/atividades/a1')
      .flush(atividadeFicticia({ tipo: 'ATENDIMENTO_TECNICO_INDIVIDUALIZADO' }));
    await fixture.whenStable();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Atendimento técnico individualizado');
    expect(texto).toContain('cada abertura fica no log de auditoria');
  });

  it('mostra a atividade e corrige mantendo o participante desligado', async () => {
    const fixture = TestBed.createComponent(FichaAtividade);
    fixture.componentRef.setInput('id', 'a1');
    await fixture.whenStable();
    controle
      .expectOne('/api/atividades/opcoes')
      .flush({ usuarios: [], profissionais: [], sugeridoId: null });
    controle.expectOne('/api/atividades/a1').flush(atividadeFicticia());
    await fixture.whenStable();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Oficina de música');
    expect(texto).toContain('Ana Fictícia');
    expect(texto).toContain('Roda de música (fictício).');

    const ficha = fixture.componentInstance as unknown as FichaExposta;
    ficha.corrigir();
    ficha.formulario.patchValue({ titulo: 'Oficina de canto' });
    ficha.salvar();

    const pedido = controle.expectOne('/api/atividades/a1');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body).toMatchObject({
      titulo: 'Oficina de canto',
      participantes: ['p1'],
    });
    pedido.flush(atividadeFicticia({ titulo: 'Oficina de canto' }));
  });
});
