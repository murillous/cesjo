import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { parceiroFicticio } from '../parcerias/parceiro-ficticio';
import { eventoFicticio } from './evento-ficticio';
import { FormularioEvento } from './formulario-evento';

interface Exposto {
  formulario: {
    patchValue(v: object): void;
    disabled: boolean;
    controls: { direcao: { value: string } };
  };
  direcoes(): readonly unknown[];
  salvar(): void;
}

describe('FormularioEvento', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioEvento],
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

  function entrar(permissoes = ['EVENTOS_LER', 'EVENTOS_REGISTRAR', 'PARCERIAS_LER'] as const) {
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes: [...permissoes] }));
  }

  function responderParceiros(): void {
    controle
      .expectOne((r) => r.url === '/api/parceiros')
      .flush({
        content: [parceiroFicticio({ id: 'p1', nome: 'CREAS Fictício' })],
        page: { size: 200, number: 0, totalElements: 1, totalPages: 1 },
      });
  }

  it('registra a visita recebida ligada ao parceiro', async () => {
    entrar();
    const fixture = TestBed.createComponent(FormularioEvento);
    responderParceiros();
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as Exposto;

    componente.formulario.patchValue({
      tipo: 'VISITA_INSTITUCIONAL',
      data: new Date(2026, 3, 15),
      descricao: ' Visita técnica. ',
      parceiroId: 'p1',
    });
    componente.salvar();
    controle.expectNone('/api/eventos-institucionais');

    componente.formulario.patchValue({ direcao: 'RECEBIDO' });
    componente.salvar();
    const pedido = controle.expectOne('/api/eventos-institucionais');
    expect(pedido.request.body).toEqual({
      tipo: 'VISITA_INSTITUCIONAL',
      direcao: 'RECEBIDO',
      data: '2026-04-15',
      descricao: 'Visita técnica.',
      participantes: null,
      parceiroId: 'p1',
    });
    pedido.flush(eventoFicticio({ id: 'e9' }));

    expect(navegar).toHaveBeenCalledWith(['/eventos', 'e9']);
  });

  it('só oferece indicação para visitas e eventos, e limpa ao trocar de tipo', async () => {
    entrar();
    const fixture = TestBed.createComponent(FormularioEvento);
    responderParceiros();
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as Exposto;

    componente.formulario.patchValue({ tipo: 'EVENTO', direcao: 'PARTICIPADO' });
    expect(componente.direcoes()).toHaveLength(2);

    componente.formulario.patchValue({ tipo: 'REUNIAO_DIRECAO_GERAL' });
    expect(componente.direcoes()).toHaveLength(0);
    expect(componente.formulario.controls.direcao.value).toBe('');
  });

  it('só mostra os dados para quem não registra', async () => {
    entrar(['EVENTOS_LER'] as unknown as ['EVENTOS_LER', 'EVENTOS_REGISTRAR', 'PARCERIAS_LER']);
    const fixture = TestBed.createComponent(FormularioEvento);
    fixture.componentRef.setInput('id', 'e1');
    await fixture.whenStable();
    controle
      .expectOne('/api/eventos-institucionais/e1')
      .flush(eventoFicticio({ id: 'e1', parceiro: { id: 'p1', nome: 'CREAS Fictício' } }));
    await fixture.whenStable();

    expect((fixture.componentInstance as unknown as Exposto).formulario.disabled).toBe(true);
    const texto = (fixture.nativeElement as HTMLElement).textContent;
    expect(texto).not.toContain('Salvar registro');
  });
});
