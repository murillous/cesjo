import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ArquivoEnviado, ArquivosApi, EventoEnvio, nomeDoContentDisposition } from './arquivos-api';
import { arquivoDeTeste, BYTES_PDF } from './arquivos-de-teste';

const ENVIADO: ArquivoEnviado = {
  id: '6f1c2c2e-1b7a-4f3e-9d47-0c0d0e0f1a2b',
  nome: 'laudo-ficticio.pdf',
  tipoMime: 'application/pdf',
  tamanho: BYTES_PDF.length,
  sha256: 'a'.repeat(64),
  enviadoEm: '2026-11-20T12:00:00Z',
};

describe('ArquivosApi', () => {
  let api: ArquivosApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ArquivosApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia o arquivo em multipart na parte "arquivo" e informa o progresso', () => {
    const eventos: EventoEnvio[] = [];
    const arquivo = arquivoDeTeste('laudo-ficticio.pdf', BYTES_PDF, 'application/pdf');

    api.enviar(arquivo).subscribe((evento) => eventos.push(evento));

    const requisicao = http.expectOne('/api/arquivos');
    expect(requisicao.request.method).toBe('POST');
    expect(requisicao.request.reportProgress).toBe(true);
    const corpo = requisicao.request.body as FormData;
    expect((corpo.get('arquivo') as File).name).toBe('laudo-ficticio.pdf');

    requisicao.event({ type: HttpEventType.Sent });
    requisicao.event({ type: HttpEventType.UploadProgress, loaded: 4, total: 8 });
    requisicao.flush(ENVIADO, { status: 201, statusText: 'Created' });

    expect(eventos).toEqual([
      { tipo: 'progresso', percentual: 50 },
      { tipo: 'concluido', arquivo: ENVIADO },
    ]);
  });

  it('baixa o arquivo com o nome do Content-Disposition', () => {
    let nome = '';
    let conteudo: Blob | undefined;

    api.baixar(ENVIADO.id).subscribe((arquivo) => {
      nome = arquivo.nome;
      conteudo = arquivo.conteudo;
    });

    const requisicao = http.expectOne(`/api/arquivos/${ENVIADO.id}`);
    expect(requisicao.request.method).toBe('GET');
    expect(requisicao.request.responseType).toBe('blob');
    requisicao.flush(new Blob([new Uint8Array(BYTES_PDF)]), {
      headers: {
        'Content-Disposition':
          'attachment; filename="foto acao.png"; filename*=UTF-8\'\'foto%20a%C3%A7%C3%A3o.png',
      },
    });

    expect(nome).toBe('foto ação.png');
    expect(conteudo?.size).toBe(BYTES_PDF.length);
  });

  it('baixa e entrega ao navegador para salvar', () => {
    const criar = vi.fn(() => 'blob:teste');
    const revogar = vi.fn();
    const clique = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
    Object.assign(URL, { createObjectURL: criar, revokeObjectURL: revogar });

    api.baixarESalvar(ENVIADO.id).subscribe();
    http
      .expectOne(`/api/arquivos/${ENVIADO.id}`)
      .flush(new Blob(['x']), {
        headers: { 'Content-Disposition': 'attachment; filename="a.pdf"' },
      });

    expect(criar).toHaveBeenCalled();
    expect(clique).toHaveBeenCalled();
    expect(revogar).toHaveBeenCalledWith('blob:teste');
    clique.mockRestore();
  });

  it('lê o nome do Content-Disposition', () => {
    expect(nomeDoContentDisposition("attachment; filename*=UTF-8''laudo%20m%C3%A9dico.pdf")).toBe(
      'laudo médico.pdf',
    );
    expect(nomeDoContentDisposition('attachment; filename="simples.pdf"')).toBe('simples.pdf');
    expect(nomeDoContentDisposition('attachment; filename=sem-aspas.png')).toBe('sem-aspas.png');
    expect(nomeDoContentDisposition(null)).toBeNull();
  });
});
