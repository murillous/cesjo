import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { FichaCandidato } from './ficha-candidato';
import { Inscricao } from './lista-espera-api';

const URL = '/api/lista-espera/inscricoes/i1';

function inscricao(mudancas: Partial<Inscricao> = {}): Inscricao {
  return {
    id: 'i1',
    pessoaId: 'p1',
    nomeCompleto: 'Pessoa Fictícia',
    dataNascimento: '1990-05-10',
    tipoDeficiencia: 'TEA',
    telefone: '31987654321',
    nomeResponsavel: 'Responsável Fictício',
    origemEncaminhamento: 'CRAS',
    dataCadastro: '2026-11-02',
    situacao: 'AGUARDANDO',
    criadoEm: '2026-11-02T12:00:00Z',
    documentos: [],
    documentosPendentes: ['LAUDO_MEDICO', 'COMPROVANTE_RESIDENCIA'],
    cadastroCompleto: false,
    posicao: 4,
    historico: [
      {
        situacaoAnterior: null,
        situacaoNova: 'AGUARDANDO',
        data: '2026-11-02',
        motivoSaida: null,
        observacao: null,
        registradoEm: '2026-11-02T12:00:00Z',
      },
    ],
    ...mudancas,
  };
}

interface Componente {
  mudanca: { setValue(v: object): void };
  abrirMudanca(): void;
  confirmarMudanca(): void;
}

describe('FichaCandidato', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FichaCandidato],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function renderizar(dados = inscricao(), permissoes?: Permissao[]) {
    TestBed.inject(Sessao).definir(contaDeTeste(permissoes ? { permissoes } : {}));
    const fixture = TestBed.createComponent(FichaCandidato);
    fixture.componentRef.setInput('id', 'i1');
    fixture.detectChanges();
    http.expectOne(URL).flush(dados);
    await fixture.whenStable();
    return {
      fixture,
      elemento: fixture.nativeElement as HTMLElement,
      componente: fixture.componentInstance as unknown as Componente,
    };
  }

  it('mostra posição, dados, documentos pendentes e histórico', async () => {
    const { elemento } = await renderizar();

    expect(elemento.querySelector('.lugar')?.textContent).toContain('4º');
    expect(elemento.querySelector('h1')?.textContent).toContain('Pessoa Fictícia');
    expect(elemento.querySelector('.dados')?.textContent).toContain('(31) 98765-4321');
    expect(elemento.querySelector('.dados')?.textContent).toContain('10/05/1990');
    expect(elemento.querySelector('.estado')?.textContent).toContain(
      'falta laudo médico e comprovante de residência',
    );
    expect(elemento.querySelector('.historico')?.textContent).toContain('Entrou na fila');
    expect(elemento.querySelector('a[href$="/editar"]')).not.toBeNull();
  });

  it('tira da fila com data e motivo', async () => {
    const { fixture, componente, elemento } = await renderizar();
    componente.abrirMudanca();
    componente.mudanca.setValue({
      data: new Date(2026, 10, 10),
      motivoSaida: 'DESISTENCIA',
      observacao: ' Família mudou de cidade. ',
    });
    componente.confirmarMudanca();

    const pedido = http.expectOne(`${URL}/situacao`);
    expect(pedido.request.body).toEqual({
      situacao: 'SAIU',
      data: '2026-11-10',
      motivoSaida: 'DESISTENCIA',
      observacao: 'Família mudou de cidade.',
    });
    pedido.flush(
      inscricao({
        situacao: 'SAIU',
        posicao: null,
        dataSaida: '2026-11-10',
        motivoSaida: 'DESISTENCIA',
        historico: [
          ...inscricao().historico,
          {
            situacaoAnterior: 'AGUARDANDO',
            situacaoNova: 'SAIU',
            data: '2026-11-10',
            motivoSaida: 'DESISTENCIA',
            observacao: 'Família mudou de cidade.',
            registradoEm: '2026-11-10T12:00:00Z',
          },
        ],
      }),
    );
    await fixture.whenStable();

    expect(elemento.querySelector('.lugar')?.textContent).toContain('Saiu da lista');
    expect(elemento.querySelector('.historico li')?.textContent).toContain(
      'Saiu da lista: desistência',
    );
    expect(elemento.querySelector('.mudar')?.textContent).toContain('Voltar para a fila');
  });

  it('não envia saída sem motivo nem volta sem observação', async () => {
    const { componente, fixture, elemento } = await renderizar();
    componente.abrirMudanca();
    componente.mudanca.setValue({
      data: new Date(2026, 10, 10),
      motivoSaida: null,
      observacao: '',
    });
    componente.confirmarMudanca();
    http.expectNone(`${URL}/situacao`);
    await fixture.whenStable();
    expect(elemento.querySelector('.mudanca [role="alert"]')?.textContent).toContain(
      'Escolha o motivo da saída.',
    );
  });

  it('sem LISTA_ESPERA_EDITAR, não mostra editar nem mudar situação', async () => {
    const { elemento } = await renderizar(inscricao(), ['LISTA_ESPERA_LER']);
    expect(elemento.querySelector('a[href$="/editar"]')).toBeNull();
    expect(elemento.querySelector('.mudar')).toBeNull();
  });

  it('em avaliação mantém a posição e pode sair da fila', async () => {
    const { elemento } = await renderizar(inscricao({ situacao: 'EM_AVALIACAO', posicao: 2 }));
    expect(elemento.querySelector('.lugar')?.textContent).toContain('2º');
    expect(elemento.querySelector('.lugar')?.textContent).toContain('em avaliação');
    expect(elemento.querySelector('.mudar')?.textContent).toContain('Tirar da fila');
  });

  it('com ATENDIDOS_INSERIR, leva à inserção no serviço quem está aguardando', async () => {
    const { elemento } = await renderizar(inscricao(), ['LISTA_ESPERA_LER', 'ATENDIDOS_INSERIR']);
    expect(elemento.querySelector('a.inserir')?.getAttribute('href')).toBe('/atendidos/inserir/i1');
  });

  it('com ATENDIDOS_INSERIR, também leva à inserção quem está em avaliação', async () => {
    const { elemento } = await renderizar(inscricao({ situacao: 'EM_AVALIACAO', posicao: 1 }), [
      'LISTA_ESPERA_LER',
      'ATENDIDOS_INSERIR',
    ]);
    expect(elemento.querySelector('a.inserir')?.getAttribute('href')).toBe('/atendidos/inserir/i1');
  });

  it('sem ATENDIDOS_INSERIR, não mostra a inserção', async () => {
    const { elemento } = await renderizar(inscricao(), ['LISTA_ESPERA_LER']);
    expect(elemento.querySelector('a.inserir')).toBeNull();
  });

  it('inserido não muda de situação pela ficha', async () => {
    const { elemento } = await renderizar(inscricao({ situacao: 'INSERIDO', posicao: null }));
    expect(elemento.querySelector('.mudar')).toBeNull();
    expect(elemento.querySelector('.lugar')?.textContent).toContain('Inserido no serviço');
  });
});
