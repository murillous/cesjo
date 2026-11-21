import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';

import { ArquivoBaixado } from '../arquivos/arquivos-api';
import { BotaoExportar } from './botao-exportar';
import { ParametrosExportacao } from './exportacao-api';

@Component({
  imports: [BotaoExportar],
  template: `<app-botao-exportar
    url="/api/teste/exportacao"
    [parametros]="filtros()"
    (exportado)="exportados.push($event)"
  />`,
})
class Hospedeiro {
  readonly filtros = signal<ParametrosExportacao>({ situacao: 'AGUARDANDO' });
  readonly exportados: ArquivoBaixado[] = [];
}

describe('BotaoExportar', () => {
  let http: HttpTestingController;
  let clique: ReturnType<typeof vi.spyOn>;

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
    Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:teste'), revokeObjectURL: vi.fn() });
    clique = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
  });

  afterEach(() => {
    http.verify();
    clique.mockRestore();
  });

  async function renderizar() {
    const fixture = TestBed.createComponent(Hospedeiro);
    await fixture.whenStable();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  async function escolher(
    fixture: Awaited<ReturnType<typeof renderizar>>['fixture'],
    texto: string,
  ) {
    const elemento = fixture.nativeElement as HTMLElement;
    elemento.querySelector<HTMLButtonElement>('button.abrir')!.click();
    await fixture.whenStable();
    const opcao = Array.from(document.querySelectorAll<HTMLButtonElement>('[mat-menu-item]')).find(
      (botao) => botao.textContent?.includes(texto),
    );
    expect(opcao).toBeDefined();
    opcao!.click();
    await fixture.whenStable();
  }

  it('oferece PDF e planilha e baixa o PDF com os filtros da tela', async () => {
    const { fixture, elemento } = await renderizar();

    await escolher(fixture, 'PDF');

    const botao = elemento.querySelector<HTMLButtonElement>('button.abrir')!;
    expect(botao.disabled).toBe(true);
    expect(elemento.querySelector('.situacao')?.textContent).toContain('Gerando o PDF');

    const requisicao = http.expectOne((r) => r.url === '/api/teste/exportacao');
    expect(requisicao.request.params.get('formato')).toBe('pdf');
    expect(requisicao.request.params.get('situacao')).toBe('AGUARDANDO');
    requisicao.flush(new Blob(['%PDF-']), {
      headers: { 'Content-Disposition': 'attachment; filename="lista.pdf"' },
    });
    await fixture.whenStable();

    expect(fixture.componentInstance.exportados.map((a) => a.nome)).toEqual(['lista.pdf']);
    expect(clique).toHaveBeenCalled();
    expect(botao.disabled).toBe(false);
  });

  it('mostra erro de permissão e libera o botão', async () => {
    const { fixture, elemento } = await renderizar();

    await escolher(fixture, 'Planilha Excel');
    const requisicao = http.expectOne((r) => r.url === '/api/teste/exportacao');
    expect(requisicao.request.params.get('formato')).toBe('xlsx');
    requisicao.flush(new Blob(['']), { status: 403, statusText: 'Forbidden' });
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'não tem permissão para exportar',
    );
    expect(elemento.querySelector<HTMLButtonElement>('button.abrir')!.disabled).toBe(false);
    expect(fixture.componentInstance.exportados).toEqual([]);
  });
});
