import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';

import { AtendidoBasico, FichaCompleta } from './atendidos-api';
import { FichaAtendidoPagina } from './ficha-atendido';

const FICHA: FichaCompleta = {
  completo: true,
  id: 'ins-1',
  pessoaId: 'pes-1',
  nomeCompleto: 'Ana Fictícia',
  situacao: 'ATIVA',
  turno: 'VESPERTINO',
  dataInsercao: '2026-11-10',
  tipoDeficiencia: 'T21',
  dataNascimento: '2004-03-15',
  idade: 22,
  faixaEtaria: 'JOVEM',
  genero: 'FEMININO',
  bairro: 'Centro',
  telefone: '31987654321',
  nomeCuidador: 'Cuidadora Fictícia',
  inscricao: { id: 'insc-1', dataCadastro: '2026-11-02', origemEncaminhamento: 'CREAS' },
  dataDesligamento: '2026-11-20',
  desligamento: {
    id: 'des-1',
    data: '2026-11-20',
    motivo: 'MUDANCA_MUNICIPIO',
    observacao: 'Observação fictícia.',
  },
  cadastro: {
    genero: 'FEMININO',
    cpf: '52998224725',
    nis: null,
    rg: null,
    escolaridade: 'MEDIO_COMPLETO',
    cep: '36000000',
    logradouro: 'Rua Fictícia',
    numero: '10',
    complemento: null,
    bairro: 'Centro',
    cidade: 'Juiz de Fora',
    rendaFamiliar: 1518.5,
    recebeBpc: true,
    inscritoCadUnico: true,
    recebeBolsaFamilia: false,
  },
  dadosSensiveis: {
    crencaReligiosa: 'MATRIZ_AFRICANA',
    medicamentosUsoContinuo: 'Remédio fictício',
    observacoesSaude: null,
  },
};

describe('FichaAtendidoPagina', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FichaAtendidoPagina],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function abrir(resposta: object, status = 200) {
    const fixture = TestBed.createComponent(FichaAtendidoPagina);
    fixture.componentRef.setInput('id', 'ins-1');
    fixture.detectChanges();
    http
      .expectOne('/api/atendidos/ins-1')
      .flush(resposta, { status, statusText: status === 200 ? 'OK' : 'Erro' });
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('mostra as atividades da pessoa para quem lê atividades', async () => {
    TestBed.inject(Sessao).definir(
      contaDeTeste({ permissoes: ['ATENDIDOS_LER', 'ATIVIDADES_LER'] }),
    );
    const fixture = TestBed.createComponent(FichaAtendidoPagina);
    fixture.componentRef.setInput('id', 'ins-1');
    fixture.detectChanges();
    http.expectOne('/api/atendidos/ins-1').flush(FICHA);
    await fixture.whenStable();

    const pedido = http.expectOne((r) => r.url === '/api/atividades');
    expect(pedido.request.params.get('pessoaId')).toBe('pes-1');
    pedido.flush({
      content: [
        {
          id: 'a1',
          tipo: 'COM_USUARIOS',
          data: '2026-11-18',
          titulo: 'Oficina de pintura',
          participantes: [],
          profissionais: [],
        },
      ],
      page: { size: 5, number: 0, totalElements: 7, totalPages: 2 },
    });
    await fixture.whenStable();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Oficina de pintura');
    expect(texto).toContain('Ver todas as 7 atividades');
  });

  it('mostra a ficha completa com pessoa, serviço e lista de espera', async () => {
    const elemento = await abrir(FICHA);

    expect(elemento.querySelector('h1')?.textContent).toContain('Ana Fictícia');
    expect(elemento.textContent).toContain('Ativo desde 10/11/2026');
    expect(elemento.textContent).toContain('Síndrome de Down (T21)');
    expect(elemento.textContent).toContain('22 anos');
    expect(elemento.textContent).toContain('Jovem (18 a 29 anos)');
    expect(elemento.textContent).toContain('(31) 98765-4321');
    expect(elemento.textContent).toContain('Cuidadora Fictícia');
    expect(elemento.textContent).toContain('CREAS');
    expect(elemento.textContent).toContain('Mudança para outro município');
    expect(elemento.textContent).toContain('Observação fictícia.');
    expect(elemento.textContent).toContain('Mudança para outro município');
    expect(elemento.textContent).toContain('Observação fictícia.');
  });

  it('mostra o cadastro completo, com crença e saúde quando a API manda', async () => {
    const elemento = await abrir(FICHA);

    expect(elemento.textContent).toContain('Feminino');
    expect(elemento.textContent).toContain('52998224725');
    expect(elemento.textContent).toContain('Rua Fictícia, 10 – Juiz de Fora – CEP 36000-000');
    expect(elemento.textContent).toContain('Médio completo');
    expect(elemento.textContent).toContain('BPC, CadÚnico');
    expect(elemento.textContent).toContain('Matriz africana');
    expect(elemento.textContent).toContain('Remédio fictício');
  });

  it('não mostra crença e saúde sem os dados sensíveis', async () => {
    const elemento = await abrir({ ...FICHA, dadosSensiveis: null });

    expect(elemento.textContent).toContain('Documentos e endereço');
    expect(elemento.textContent).not.toContain('Crença religiosa');
  });

  it('mostra só os dados básicos quando a API devolve a visão básica', async () => {
    const basico: AtendidoBasico = {
      completo: false,
      id: 'ins-1',
      nomeCompleto: 'Ana Fictícia',
      situacao: 'ATIVA',
      turno: 'MATUTINO',
      dataInsercao: '2026-11-10',
      dataDesligamento: '2026-11-20',
    };
    const elemento = await abrir(basico);

    expect(elemento.textContent).toContain('Matutino');
    expect(elemento.textContent).toContain('Seu perfil vê só nome, situação, turno e datas');
    expect(elemento.textContent).not.toContain('Telefone');
    expect(elemento.textContent).toContain('Desligado em 20/11/2026');
    expect(elemento.textContent).toContain('Desligado em 20/11/2026');
  });

  it('mostra o erro quando a ficha não existe', async () => {
    const elemento = await abrir({ detail: 'Usuário atendido não encontrado.' }, 404);

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('não encontrado');
  });
});
