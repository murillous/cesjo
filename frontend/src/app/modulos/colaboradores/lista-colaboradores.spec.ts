import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { Pagina } from '../contas/contas-api';
import { colaboradorFicticio } from './colaborador-ficticio';
import { Colaborador } from './colaboradores-api';
import { ListaColaboradores } from './lista-colaboradores';

function pagina(content: Colaborador[]): Pagina<Colaborador> {
  return { content, page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 } };
}

describe('ListaColaboradores', () => {
  let controle: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ListaColaboradores],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    TestBed.inject(Sessao).definir(contaDeTeste());
  });

  afterEach(() => controle.verify());

  it('começa pelos ativos e mostra função e conta', async () => {
    const fixture = TestBed.createComponent(ListaColaboradores);
    const pedido = controle.expectOne((r) => r.url === '/api/colaboradores');
    expect(pedido.request.params.get('ativo')).toBe('true');
    expect(pedido.request.params.get('sort')).toBe('nome');
    pedido.flush(
      pagina([
        colaboradorFicticio(),
        colaboradorFicticio({
          id: 'c2',
          nome: 'Outro Fictício',
          funcao: 'PSICOLOGO',
          conta: { id: 'a1', login: 'outro.ficticio', nome: 'Outro Fictício', ativa: true },
        }),
      ]),
    );
    await fixture.whenStable();

    const linhas = (fixture.nativeElement as HTMLElement).querySelectorAll('a.linha');
    expect(linhas).toHaveLength(2);
    expect(linhas[0].textContent).toContain('Assistente social');
    expect(linhas[0].textContent).toContain('Sem conta de acesso');
    expect(linhas[1].textContent).toContain('Psicólogo(a)');
    expect(linhas[1].textContent).toContain('outro.ficticio');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Novo colaborador');
  });

  it('troca o filtro de situação', async () => {
    const fixture = TestBed.createComponent(ListaColaboradores);
    controle.expectOne((r) => r.url === '/api/colaboradores').flush(pagina([]));
    await fixture.whenStable();

    const botoes = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      'mat-button-toggle button',
    );
    botoes[2].click();
    const todos = controle.expectOne((r) => r.url === '/api/colaboradores');
    expect(todos.request.params.has('ativo')).toBe(false);
    todos.flush(pagina([colaboradorFicticio({ ativo: false })]));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('a.linha')?.textContent).toContain(
      'Inativo',
    );
  });

  it('esconde o cadastro de quem só lê', async () => {
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes: ['COLABORADORES_LER'] }));
    const fixture = TestBed.createComponent(ListaColaboradores);
    controle.expectOne((r) => r.url === '/api/colaboradores').flush(pagina([]));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Novo colaborador');
  });
});
