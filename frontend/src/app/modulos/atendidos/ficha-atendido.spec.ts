import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

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
  telefone: '31987654321',
  nomeCuidador: 'Cuidadora Fictícia',
  inscricao: { id: 'insc-1', dataCadastro: '2026-11-02', origemEncaminhamento: 'CREAS' },
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
  });

  it('mostra só os dados básicos quando a API devolve a visão básica', async () => {
    const basico: AtendidoBasico = {
      completo: false,
      id: 'ins-1',
      nomeCompleto: 'Ana Fictícia',
      situacao: 'ATIVA',
      turno: 'MATUTINO',
      dataInsercao: '2026-11-10',
    };
    const elemento = await abrir(basico);

    expect(elemento.textContent).toContain('Matutino');
    expect(elemento.textContent).toContain('Seu perfil vê só nome, situação, turno e datas');
    expect(elemento.textContent).not.toContain('Telefone');
  });

  it('mostra o erro quando a ficha não existe', async () => {
    const elemento = await abrir({ detail: 'Usuário atendido não encontrado.' }, 404);

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('não encontrado');
  });
});
