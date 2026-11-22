import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { FichaCompleta } from './atendidos-api';
import { FormularioCadastro } from './formulario-cadastro';

const FICHA: FichaCompleta = {
  completo: true,
  id: 'ins-1',
  pessoaId: 'pes-1',
  nomeCompleto: 'Ana Fictícia',
  situacao: 'ATIVA',
  turno: 'VESPERTINO',
  dataInsercao: '2026-11-10',
  dataDesligamento: null,
  tipoDeficiencia: 'T21',
  dataNascimento: '2004-03-15',
  idade: 22,
  faixaEtaria: 'JOVEM',
  genero: 'FEMININO',
  bairro: 'Centro',
  telefone: '31987654321',
  nomeCuidador: 'Cuidadora Fictícia',
  inscricao: null,
  desligamento: null,
  cadastro: {
    genero: 'FEMININO',
    cpf: null,
    nis: null,
    rg: null,
    escolaridade: null,
    cep: null,
    logradouro: null,
    numero: null,
    complemento: null,
    bairro: 'Centro',
    cidade: null,
    rendaFamiliar: null,
    recebeBpc: false,
    inscritoCadUnico: false,
    recebeBolsaFamilia: false,
  },
  dadosSensiveis: { crencaReligiosa: 'CATOLICA', medicamentosUsoContinuo: null, observacoesSaude: null },
};

interface Componente {
  formulario: {
    controls: {
      cadastro: { patchValue(v: object): void };
      dadosSensiveis: { patchValue(v: object): void };
      dataNascimento: { setValue(v: Date | null): void };
    };
  };
  atualizarFaixa(): void;
  salvar(): void;
}

describe('FormularioCadastro', () => {
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  function configurar(permissoes: Permissao[]) {
    TestBed.configureTestingModule({
      imports: [FormularioCadastro],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes }));
    http = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  }

  afterEach(() => http.verify());

  async function abrir(ficha: object = FICHA) {
    const fixture = TestBed.createComponent(FormularioCadastro);
    fixture.componentRef.setInput('id', 'ins-1');
    fixture.detectChanges();
    http.expectOne('/api/atendidos/ins-1').flush(ficha);
    await fixture.whenStable();
    fixture.detectChanges();
    return {
      elemento: fixture.nativeElement as HTMLElement,
      componente: fixture.componentInstance as unknown as Componente,
    };
  }

  it('envia o cadastro com crença e saúde para quem tem permissão', async () => {
    configurar(['ATENDIDOS_LER', 'ATENDIDOS_EDITAR', 'ATENDIDOS_DADOS_SENSIVEIS']);
    const { elemento, componente } = await abrir();

    expect(elemento.textContent).toContain('Crença religiosa e saúde');
    expect(elemento.textContent).toContain('Jovem (18 a 29 anos)');
    componente.formulario.controls.cadastro.patchValue({ cpf: '529.982.247-25', bairro: ' Vila Fictícia ' });
    componente.formulario.controls.dadosSensiveis.patchValue({ medicamentosUsoContinuo: 'Remédio fictício' });
    componente.salvar();

    const pedido = http.expectOne('/api/atendidos/ins-1/cadastro');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body.dataNascimento).toBe('2004-03-15');
    expect(pedido.request.body.cadastro.cpf).toBe('529.982.247-25');
    expect(pedido.request.body.cadastro.bairro).toBe('Vila Fictícia');
    expect(pedido.request.body.cadastro.rg).toBeNull();
    expect(pedido.request.body.dadosSensiveis).toEqual({
      crencaReligiosa: 'CATOLICA',
      medicamentosUsoContinuo: 'Remédio fictício',
      observacoesSaude: null,
    });
    pedido.flush(null, { status: 204, statusText: 'No Content' });
    expect(navegar).toHaveBeenCalledWith(['/atendidos', 'ins-1']);
  });

  it('não mostra nem envia crença e saúde sem a permissão', async () => {
    configurar(['ATENDIDOS_LER', 'ATENDIDOS_EDITAR']);
    const { elemento, componente } = await abrir({ ...FICHA, dadosSensiveis: null });

    expect(elemento.textContent).not.toContain('Crença religiosa');
    componente.salvar();

    const pedido = http.expectOne('/api/atendidos/ins-1/cadastro');
    expect(pedido.request.body.dadosSensiveis).toBeNull();
    pedido.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('recalcula a faixa etária ao mudar a data de nascimento', async () => {
    configurar(['ATENDIDOS_LER', 'ATENDIDOS_EDITAR']);
    const { componente } = await abrir();
    const hoje = new Date();

    componente.formulario.controls.dataNascimento.setValue(
      new Date(hoje.getFullYear() - 40, hoje.getMonth(), hoje.getDate()),
    );
    componente.atualizarFaixa();

    expect((componente as unknown as { faixa(): string }).faixa()).toBe('Adulto (30 a 59 anos)');
  });

  it('não envia CPF com número errado de dígitos', async () => {
    configurar(['ATENDIDOS_LER', 'ATENDIDOS_EDITAR']);
    const { componente } = await abrir();

    componente.formulario.controls.cadastro.patchValue({ cpf: '123.456' });
    componente.salvar();

    http.expectNone('/api/atendidos/ins-1/cadastro');
  });
});
