import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { FormularioVisita } from './formulario-visita';
import { Candidato } from './visitas-api';
import { visitaFicticia } from './visita-ficticia';

const CANDIDATO: Candidato = {
  inscricaoId: 'i1',
  pessoaId: 'p1',
  nomeCompleto: 'Candidata Fictícia',
  dataCadastro: '2026-11-02',
  situacao: 'AGUARDANDO',
};

interface FormularioExposto {
  formulario: {
    patchValue(v: object): void;
    controls: { candidato: { hasError(e: string): boolean } };
  };
  sugestoes(): Candidato[];
  salvar(): void;
}

describe('FormularioVisita', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioVisita],
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

  async function criar(): Promise<FormularioExposto> {
    const fixture = TestBed.createComponent(FormularioVisita);
    controle
      .expectOne('/api/visitas/candidatos')
      .flush([
        CANDIDATO,
        { ...CANDIDATO, inscricaoId: 'i2', pessoaId: 'p2', nomeCompleto: 'João Fictício' },
      ]);
    controle
      .expectOne('/api/visitas/profissionais')
      .flush([{ id: 'c1', nome: 'Colaboradora Fictícia', funcao: 'PSICOLOGO' }]);
    await fixture.whenStable();
    return fixture.componentInstance as unknown as FormularioExposto;
  }

  it('busca o candidato pelo nome, sem acento', async () => {
    const componente = await criar();
    componente.formulario.patchValue({ candidato: 'joao' });

    expect(componente.sugestoes().map((c) => c.nomeCompleto)).toEqual(['João Fictício']);
  });

  it('agenda a visita com data e hora locais e volta para a semana dela', async () => {
    const componente = await criar();
    componente.formulario.patchValue({
      candidato: CANDIDATO,
      data: new Date(2026, 10, 25),
      hora: '14:30',
      profissionais: ['c1'],
    });
    componente.salvar();

    const pedido = controle.expectOne('/api/visitas');
    expect(pedido.request.body).toEqual({
      pessoaId: 'p1',
      dataHora: '2026-11-25T14:30',
      profissionais: ['c1'],
    });
    pedido.flush(visitaFicticia());

    expect(navegar).toHaveBeenCalledWith(['/visitas'], { queryParams: { semana: '2026-11-25' } });
  });

  it('exige escolher o candidato na lista', async () => {
    const componente = await criar();
    componente.formulario.patchValue({
      candidato: 'Nome digitado',
      data: new Date(2026, 10, 25),
      hora: '14:30',
      profissionais: ['c1'],
    });
    componente.salvar();

    controle.expectNone('/api/visitas');
    expect(componente.formulario.controls.candidato.hasError('naoEscolhido')).toBe(true);
  });
});
