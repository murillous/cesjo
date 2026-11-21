import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { DocumentosCandidato } from './documentos-candidato';
import { Inscricao } from './lista-espera-api';

const URL = '/api/lista-espera/inscricoes/i1';

function inscricao(mudancas: Partial<Inscricao> = {}): Inscricao {
  return {
    id: 'i1',
    pessoaId: 'p1',
    nomeCompleto: 'Pessoa Fictícia',
    dataNascimento: null,
    tipoDeficiencia: 'TEA',
    telefone: '31987654321',
    nomeResponsavel: 'Responsável Fictício',
    origemEncaminhamento: 'CRAS',
    dataCadastro: '2026-11-02',
    situacao: 'AGUARDANDO',
    criadoEm: '2026-11-02T12:00:00Z',
    documentos: [
      {
        tipo: 'LAUDO_MEDICO',
        nomeArquivo: 'laudo-ficticio.pdf',
        tipoMime: 'application/pdf',
        tamanho: 2048,
        anexadoEm: '2026-11-02T12:00:00Z',
      },
    ],
    documentosPendentes: ['COMPROVANTE_RESIDENCIA'],
    cadastroCompleto: false,
    ...mudancas,
  };
}

describe('DocumentosCandidato', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DocumentosCandidato],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function renderizar(permissoes = contaDeTeste().permissoes) {
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes }));
    const fixture = TestBed.createComponent(DocumentosCandidato);
    fixture.componentRef.setInput('id', 'i1');
    fixture.detectChanges();
    http.expectOne(URL).flush(inscricao());
    await fixture.whenStable();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('mostra o cadastro incompleto, o documento anexado e o que falta', async () => {
    const { elemento } = await renderizar();

    expect(elemento.querySelector('.estado')?.textContent).toContain(
      'Cadastro incompleto: falta comprovante de residência.',
    );
    const documentos = elemento.querySelectorAll('.documento');
    expect(documentos[0].textContent).toContain('laudo-ficticio.pdf');
    expect(documentos[0].textContent).toContain('2 KB');
    expect(documentos[1].classList).toContain('pendente');
    expect(documentos[1].textContent).toContain('Ainda não anexado.');
    expect(documentos[0].textContent).toContain('Substituir laudo médico');
    expect(documentos[1].textContent).toContain('Anexar comprovante de residência');
  });

  it('baixa o documento pelo endpoint da inscrição', async () => {
    const { elemento } = await renderizar();
    const criarUrl = vi.spyOn(URL_GLOBAL, 'createObjectURL').mockReturnValue('blob:teste');
    vi.spyOn(URL_GLOBAL, 'revokeObjectURL').mockImplementation(() => undefined);
    const clique = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);

    elemento.querySelector<HTMLButtonElement>('button.baixar')!.click();
    http.expectOne(`${URL}/documentos/LAUDO_MEDICO`).flush(new Blob(['%PDF-']), {
      headers: { 'Content-Disposition': 'attachment; filename="laudo-ficticio.pdf"' },
    });

    expect(criarUrl).toHaveBeenCalled();
    expect(clique).toHaveBeenCalled();
  });

  it('sem permissão de documentos, não mostra baixar nem anexar', async () => {
    const { elemento } = await renderizar(['LISTA_ESPERA_LER']);

    expect(elemento.querySelector('button.baixar')).toBeNull();
    expect(elemento.querySelector('app-envio-arquivo')).toBeNull();
  });

  it('liga o arquivo enviado ao candidato e atualiza a situação do cadastro', async () => {
    const { fixture, elemento } = await renderizar();
    const componente = fixture.componentInstance as unknown as {
      anexar(tipo: string, arquivoId: string, rotulo: string): void;
    };

    componente.anexar('COMPROVANTE_RESIDENCIA', 'arquivo-novo', 'Comprovante de residência');
    const pedido = http.expectOne(`${URL}/documentos/COMPROVANTE_RESIDENCIA`);
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body).toEqual({ arquivoId: 'arquivo-novo' });
    pedido.flush(inscricao({ documentosPendentes: [], cadastroCompleto: true }));
    await fixture.whenStable();

    expect(elemento.querySelector('.estado')?.textContent).toContain('Cadastro completo.');
  });
});

const URL_GLOBAL = globalThis.URL;
