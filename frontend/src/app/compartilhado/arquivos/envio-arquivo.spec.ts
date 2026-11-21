import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { By } from '@angular/platform-browser';

import { ArquivoEnviado } from './arquivos-api';
import { EnvioArquivo } from './envio-arquivo';
import { arquivoDeTeste, BYTES_JPEG, BYTES_PDF } from './arquivos-de-teste';

@Component({
  imports: [EnvioArquivo, ReactiveFormsModule],
  template: `<app-envio-arquivo
    rotulo="Laudo médico"
    [formControl]="campo"
    (enviado)="enviados.push($event)"
    (removido)="removidos = removidos + 1"
  />`,
})
class Hospedeiro {
  readonly campo = new FormControl<string | null>(null);
  readonly enviados: ArquivoEnviado[] = [];
  removidos = 0;
}

const ENVIADO: ArquivoEnviado = {
  id: '0b9e7a52-5d1c-4f5e-8a1e-2f3a4b5c6d7e',
  nome: 'laudo.pdf',
  tipoMime: 'application/pdf',
  tamanho: BYTES_PDF.length,
  sha256: 'b'.repeat(64),
  enviadoEm: '2026-11-20T12:00:00Z',
};

describe('EnvioArquivo', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Hospedeiro],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function renderizar() {
    const fixture = TestBed.createComponent(Hospedeiro);
    await fixture.whenStable();
    const componente = fixture.debugElement.query(By.directive(EnvioArquivo))
      .componentInstance as EnvioArquivo;
    const elemento = fixture.nativeElement as HTMLElement;
    return { fixture, componente, elemento, hospedeiro: fixture.componentInstance };
  }

  async function escolher(
    fixture: { whenStable(): Promise<unknown> },
    componente: EnvioArquivo,
    arquivo: File,
  ) {
    await componente.processar(arquivo);
    await fixture.whenStable();
  }

  it('mostra o rótulo, a dica de tipos e tamanho e um botão acessível por teclado', async () => {
    const { elemento } = await renderizar();
    const botao = elemento.querySelector<HTMLButtonElement>('button.escolher');
    const input = elemento.querySelector<HTMLInputElement>('input[type="file"]');

    expect(elemento.querySelector('[role="group"]')?.textContent).toContain('Laudo médico');
    expect(elemento.textContent).toContain('PDF, JPG ou PNG, até 10 MB.');
    expect(botao?.tagName).toBe('BUTTON');
    expect(botao?.getAttribute('aria-label')).toBe('Escolher arquivo: Laudo médico');
    expect(botao?.getAttribute('aria-describedby')).toContain(
      elemento.querySelector('.dica')?.id ?? 'sem-id',
    );
    expect(input?.accept).toContain('application/pdf');
    expect(input?.getAttribute('tabindex')).toBe('-1');
  });

  it('o botão abre o seletor de arquivos', async () => {
    const { elemento } = await renderizar();
    const input = elemento.querySelector<HTMLInputElement>('input[type="file"]');
    const clique = vi.spyOn(input as HTMLInputElement, 'click').mockImplementation(() => undefined);

    elemento.querySelector<HTMLButtonElement>('button.escolher')?.click();

    expect(clique).toHaveBeenCalled();
  });

  it('envia o arquivo válido, mostra nome e tamanho e define o id no formulário', async () => {
    const { fixture, componente, elemento, hospedeiro } = await renderizar();

    await escolher(fixture, componente, arquivoDeTeste('laudo.pdf', BYTES_PDF, 'application/pdf'));
    expect(elemento.querySelector('.situacao')?.textContent).toContain('Enviando');
    expect(elemento.querySelector('mat-progress-bar')).not.toBeNull();

    http.expectOne('/api/arquivos').flush(ENVIADO, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(elemento.querySelector('.nome')?.textContent).toContain('laudo.pdf');
    expect(elemento.querySelector('.situacao')?.textContent).toContain('8 bytes, enviado');
    expect(hospedeiro.campo.value).toBe(ENVIADO.id);
    expect(hospedeiro.enviados).toEqual([ENVIADO]);
    expect(elemento.querySelector('mat-progress-bar')).toBeNull();
  });

  it('recusa no cliente arquivo de tipo não aceito, sem enviar', async () => {
    const { fixture, componente, elemento, hospedeiro } = await renderizar();

    await escolher(
      fixture,
      componente,
      arquivoDeTeste('falso.pdf', [0x4d, 0x5a, 0x90], 'application/pdf'),
    );

    http.expectNone('/api/arquivos');
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Tipo de arquivo não aceito',
    );
    expect(hospedeiro.campo.value).toBeNull();
  });

  it('recusa no cliente arquivo acima de 10 MB, sem enviar', async () => {
    const { fixture, componente, elemento } = await renderizar();
    const grande = new Uint8Array(10 * 1024 * 1024 + 1);
    grande.set(BYTES_PDF);

    await escolher(fixture, componente, new File([grande], 'grande.pdf'));

    http.expectNone('/api/arquivos');
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'passa do limite de 10 MB',
    );
  });

  it('mostra a mensagem do ProblemDetail quando o backend recusa', async () => {
    const { fixture, componente, elemento, hospedeiro } = await renderizar();

    await escolher(fixture, componente, arquivoDeTeste('foto.jpg', BYTES_JPEG, 'image/jpeg'));
    http
      .expectOne('/api/arquivos')
      .flush(
        { status: 422, detail: 'O arquivo está vazio.' },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'O arquivo está vazio.',
    );
    expect(hospedeiro.campo.value).toBeNull();
  });

  it('explica a falta de permissão e a falha inesperada', async () => {
    const { fixture, componente, elemento } = await renderizar();

    await escolher(fixture, componente, arquivoDeTeste('a.pdf', BYTES_PDF));
    http.expectOne('/api/arquivos').flush(null, { status: 403, statusText: 'Forbidden' });
    await fixture.whenStable();
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Sua conta não tem permissão para enviar arquivos.',
    );

    await escolher(fixture, componente, arquivoDeTeste('a.pdf', BYTES_PDF));
    http.expectOne('/api/arquivos').flush(null, { status: 500, statusText: 'Erro' });
    await fixture.whenStable();
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Não foi possível enviar o arquivo. Tente de novo.',
    );
  });

  it('remove o arquivo enviado, limpa o formulário e avisa', async () => {
    const { fixture, componente, elemento, hospedeiro } = await renderizar();
    await escolher(fixture, componente, arquivoDeTeste('laudo.pdf', BYTES_PDF));
    http.expectOne('/api/arquivos').flush(ENVIADO);
    await fixture.whenStable();

    const remover = elemento.querySelector<HTMLButtonElement>('button.remover');
    expect(remover?.getAttribute('aria-label')).toBe('Remover laudo.pdf');
    remover?.click();
    await fixture.whenStable();

    expect(hospedeiro.campo.value).toBeNull();
    expect(hospedeiro.removidos).toBe(1);
    expect(elemento.querySelector('button.escolher')).not.toBeNull();
  });

  it('mostra arquivo já anexado quando o formulário traz um id', async () => {
    const { fixture, elemento, hospedeiro } = await renderizar();

    hospedeiro.campo.setValue(ENVIADO.id);
    await fixture.whenStable();

    expect(elemento.querySelector('.nome')?.textContent).toContain('Arquivo anexado');
    expect(elemento.querySelector('.situacao')?.textContent).toContain('Já enviado');
  });

  it('desabilita os botões quando o campo está desabilitado', async () => {
    const { fixture, elemento, hospedeiro } = await renderizar();

    hospedeiro.campo.disable();
    await fixture.whenStable();

    expect(elemento.querySelector<HTMLButtonElement>('button.escolher')?.disabled).toBe(true);
  });

  it('aceita arquivo arrastado e solto', async () => {
    const { fixture, elemento } = await renderizar();
    const area = elemento.querySelector<HTMLElement>('.envio');
    const arquivo = arquivoDeTeste('solto.pdf', BYTES_PDF);
    const soltar = new Event('drop', { bubbles: true, cancelable: true });
    Object.defineProperty(soltar, 'dataTransfer', {
      value: { files: { item: () => arquivo, length: 1 } },
    });

    area?.dispatchEvent(soltar);
    await vi.waitFor(() => http.expectOne('/api/arquivos').flush(ENVIADO));
    await fixture.whenStable();

    expect(elemento.querySelector('.nome')?.textContent).toContain('laudo.pdf');
  });
});
