import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';

import { Previa } from './importacao-api';
import { ImportacaoPlanilha } from './importacao-planilha';

const BASE = '/api/importacao/lista-espera';

const PREVIA: Previa = {
  colunas: ['Nome completo', 'Telefone', 'Data de cadastro'],
  linhas: [
    { linha: 2, valores: ['Ana Fictícia', '3132345678', '02/01/2001'], erros: [] },
    {
      linha: 3,
      valores: ['Bia Fictícia', '123', ''],
      erros: ['Telefone: informe DDD e número.', 'Preencha a coluna Data de cadastro.'],
    },
  ],
  total: 2,
  validas: 1,
  comErro: 1,
};

function planilha(nome = 'lista.xlsx', tamanho = 10): File {
  return new File([new Uint8Array(tamanho)], nome, {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
}

describe('ImportacaoPlanilha', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ImportacaoPlanilha],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function renderizar() {
    const fixture = TestBed.createComponent(ImportacaoPlanilha);
    fixture.componentRef.setInput('base', BASE);
    fixture.componentRef.setInput('item', 'candidato');
    fixture.componentRef.setInput('itens', 'candidatos');
    fixture.detectChanges();
    http.expectOne(`${BASE}/colunas`).flush([
      {
        titulo: 'Tipo de deficiência',
        obrigatoria: true,
        formato: 'Um dos valores aceitos.',
        exemplo: 'TEA',
        valoresAceitos: ['Física', 'TEA'],
      },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('explica as colunas do modelo', async () => {
    const { elemento } = await renderizar();
    expect(elemento.querySelector('.colunas')?.textContent).toContain('Física, TEA');
    expect(elemento.querySelectorAll('.passo').length).toBe(3);
  });

  it('mostra a pré-visualização com os erros por linha e confirma só as válidas', async () => {
    const { fixture, elemento } = await renderizar();
    const componente = fixture.componentInstance;
    let importadas = 0;
    componente.importado.subscribe((r) => (importadas = r.importadas));

    componente.ler(planilha());
    const previa = http.expectOne(`${BASE}/previa`);
    expect(previa.request.method).toBe('POST');
    expect((previa.request.body as FormData).get('arquivo')).toBeInstanceOf(File);
    previa.flush(PREVIA);
    fixture.detectChanges();

    expect(elemento.querySelector('.resumo')?.textContent).toContain('1 linha pronta');
    const linhas = elemento.querySelectorAll('.linha');
    expect(linhas.length).toBe(2);
    expect(linhas[1].classList).toContain('com-erro');
    expect(linhas[1].querySelectorAll('.erros li').length).toBe(2);

    const botao = Array.from(elemento.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Importar 1 linha'),
    );
    botao!.click();
    const confirmacao = http.expectOne(BASE);
    expect(confirmacao.request.method).toBe('POST');
    confirmacao.flush({ importadas: 1, ignoradas: 1 });
    fixture.detectChanges();

    expect(importadas).toBe(1);
    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      '1 candidato importado',
    );
  });

  it('recusa no cliente arquivo que não é .xlsx ou passa de 2 MB', async () => {
    const { fixture, elemento } = await renderizar();
    fixture.componentInstance.ler(planilha('lista.csv'));
    fixture.detectChanges();
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('.xlsx');

    fixture.componentInstance.ler(planilha('grande.xlsx', 2 * 1024 * 1024 + 1));
    fixture.detectChanges();
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('2 MB');
    http.expectNone(`${BASE}/previa`);
  });

  it('mostra o erro do backend para planilha malformada', async () => {
    const { fixture, elemento } = await renderizar();
    fixture.componentInstance.ler(planilha());
    http
      .expectOne(`${BASE}/previa`)
      .flush(
        { detail: 'O arquivo não é uma planilha Excel (.xlsx).', status: 422 },
        { status: 422, statusText: 'Unprocessable Entity' },
      );
    fixture.detectChanges();
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'não é uma planilha Excel',
    );
  });
});
