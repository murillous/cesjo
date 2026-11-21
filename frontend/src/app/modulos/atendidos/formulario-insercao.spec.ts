import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { CandidatoInsercao } from './atendidos-api';
import { FormularioInsercao, NOVO_CUIDADOR } from './formulario-insercao';

const INSCRICAO = '5e7a1c2d-0000-4000-8000-000000000001';

const CANDIDATO: CandidatoInsercao = {
  inscricaoId: INSCRICAO,
  pessoaId: '5e7a1c2d-0000-4000-8000-000000000002',
  nomeCompleto: 'Pessoa Fictícia',
  tipoDeficiencia: 'TEA',
  dataCadastro: '2026-11-02',
  situacao: 'AGUARDANDO',
  responsaveis: [{ id: 'resp-1', nomeCompleto: 'Responsável Fictício', principal: true }],
};

interface Componente {
  formulario: {
    setValue(v: object): void;
    controls: Record<string, { value: unknown; setValue(v: unknown): void }>;
  };
  salvar(): void;
}

describe('FormularioInsercao', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioInsercao],
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

  async function criar(candidato: CandidatoInsercao = CANDIDATO) {
    const fixture = TestBed.createComponent(FormularioInsercao);
    fixture.componentRef.setInput('inscricaoId', INSCRICAO);
    fixture.detectChanges();
    controle.expectOne(`/api/atendidos/candidatos/${INSCRICAO}`).flush(candidato);
    await fixture.whenStable();
    return {
      fixture,
      componente: fixture.componentInstance as unknown as Componente,
      elemento: fixture.nativeElement as HTMLElement,
    };
  }

  it('mostra o candidato e já escolhe o responsável do cadastro como cuidador', async () => {
    const { componente, elemento } = await criar();

    expect(elemento.textContent).toContain('Pessoa Fictícia');
    expect(elemento.textContent).toContain('Na fila desde 02/11/2026');
    expect(componente.formulario.controls['cuidador'].value).toBe('resp-1');
  });

  it('insere com o responsável escolhido e abre a ficha', async () => {
    const { componente } = await criar();
    componente.formulario.setValue({
      dataInsercao: new Date(2026, 10, 10),
      turno: 'VESPERTINO',
      cuidador: 'resp-1',
      nomeNovoCuidador: '',
    });
    componente.salvar();

    const pedido = controle.expectOne('/api/atendidos/insercoes');
    expect(pedido.request.body).toEqual({
      inscricaoId: INSCRICAO,
      dataInsercao: '2026-11-10',
      turno: 'VESPERTINO',
      cuidadorResponsavelId: 'resp-1',
      nomeNovoCuidador: null,
    });
    pedido.flush(
      { id: 'ins-1', nomeCompleto: 'Pessoa Fictícia' },
      { status: 201, statusText: 'Created' },
    );
    expect(navegar).toHaveBeenCalledWith(['/atendidos', 'ins-1']);
  });

  it('manda o nome do cuidador novo e exige o nome', async () => {
    const { componente } = await criar();
    componente.formulario.setValue({
      dataInsercao: new Date(2026, 10, 10),
      turno: 'INTEGRAL',
      cuidador: NOVO_CUIDADOR,
      nomeNovoCuidador: ' ',
    });
    componente.salvar();
    controle.expectNone('/api/atendidos/insercoes');

    componente.formulario.controls['nomeNovoCuidador'].setValue(' Cuidadora Fictícia ');
    componente.salvar();
    const pedido = controle.expectOne('/api/atendidos/insercoes');
    expect(pedido.request.body.cuidadorResponsavelId).toBeNull();
    expect(pedido.request.body.nomeNovoCuidador).toBe('Cuidadora Fictícia');
    pedido.flush({ id: 'ins-2', nomeCompleto: 'Pessoa Fictícia' });
  });

  it('mostra o detail do erro do backend', async () => {
    const { fixture, componente, elemento } = await criar();
    componente.formulario.setValue({
      dataInsercao: new Date(2026, 10, 10),
      turno: 'MATUTINO',
      cuidador: 'resp-1',
      nomeNovoCuidador: '',
    });
    componente.salvar();
    controle
      .expectOne('/api/atendidos/insercoes')
      .flush(
        { detail: 'Esta pessoa já é usuária ativa do serviço.' },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('já é usuária ativa');
    expect(navegar).not.toHaveBeenCalled();
  });

  it('não mostra o formulário para quem já saiu da fila', async () => {
    const { elemento } = await criar({ ...CANDIDATO, situacao: 'INSERIDO' });

    expect(elemento.querySelector('form')).toBeNull();
    expect(elemento.textContent).toContain('não está mais aguardando');
  });
});
