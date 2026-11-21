import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { mensagemDeErro } from '../../acesso/problema';
import { ArquivoBaixado } from '../arquivos/arquivos-api';
import { ExportacaoApi } from './exportacao-api';

const URL_TESTE = '/api/teste/exportacao';

describe('ExportacaoApi', () => {
  let api: ExportacaoApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ExportacaoApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia o formato e só os filtros preenchidos, e lê o nome do Content-Disposition', () => {
    let baixado: ArquivoBaixado | undefined;

    api
      .baixar(URL_TESTE, 'pdf', { situacao: 'AGUARDANDO', nome: '', turno: null, pagina: 0 })
      .subscribe((arquivo) => (baixado = arquivo));

    const requisicao = http.expectOne((r) => r.url === URL_TESTE);
    expect(requisicao.request.method).toBe('GET');
    expect(requisicao.request.responseType).toBe('blob');
    expect(requisicao.request.params.get('formato')).toBe('pdf');
    expect(requisicao.request.params.get('situacao')).toBe('AGUARDANDO');
    expect(requisicao.request.params.get('pagina')).toBe('0');
    expect(requisicao.request.params.has('nome')).toBe(false);
    expect(requisicao.request.params.has('turno')).toBe(false);
    requisicao.flush(new Blob(['%PDF-']), {
      headers: {
        'Content-Disposition':
          'attachment; filename="lista-de-espera-2026-11-21.pdf"; filename*=UTF-8\'\'lista-de-espera-2026-11-21.pdf',
      },
    });

    expect(baixado?.nome).toBe('lista-de-espera-2026-11-21.pdf');
    expect(baixado?.conteudo.size).toBe(5);
  });

  it('usa um nome padrão quando o backend não informa', () => {
    let nome = '';
    api.baixar(URL_TESTE, 'xlsx').subscribe((arquivo) => (nome = arquivo.nome));
    http.expectOne((r) => r.url === URL_TESTE).flush(new Blob(['x']));
    expect(nome).toBe('exportacao.xlsx');
  });

  it('converte o ProblemDetail que chega como Blob para mostrar o detail', async () => {
    const erro = new Promise<unknown>((resolve) =>
      api.baixar(URL_TESTE, 'pdf').subscribe({ error: resolve }),
    );

    http
      .expectOne((r) => r.url === URL_TESTE)
      .flush(
        new Blob([JSON.stringify({ title: 'Dados inválidos', detail: 'Período inválido.' })], {
          type: 'application/problem+json',
        }),
        { status: 400, statusText: 'Bad Request' },
      );

    const recebido = await erro;
    expect(recebido).toBeInstanceOf(HttpErrorResponse);
    expect((recebido as HttpErrorResponse).status).toBe(400);
    expect(mensagemDeErro(recebido, 'padrão')).toBe('Período inválido.');
  });

  it('baixa e entrega ao navegador para salvar', () => {
    const criar = vi.fn(() => 'blob:teste');
    const revogar = vi.fn();
    const clique = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
    Object.assign(URL, { createObjectURL: criar, revokeObjectURL: revogar });

    api.baixarESalvar(URL_TESTE, 'xlsx').subscribe();
    http.expectOne((r) => r.url === URL_TESTE).flush(new Blob(['x']));

    expect(criar).toHaveBeenCalled();
    expect(clique).toHaveBeenCalled();
    expect(revogar).toHaveBeenCalledWith('blob:teste');
    clique.mockRestore();
  });
});
