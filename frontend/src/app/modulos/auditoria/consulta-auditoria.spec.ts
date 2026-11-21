import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';

import { PaginaAuditoria, RegistroAuditoria } from './auditoria-api';
import { ConsultaAuditoria } from './consulta-auditoria';

const REGISTRO: RegistroAuditoria = {
  id: '00000000-0000-0000-0000-000000000001',
  ocorridoEm: '2026-11-20T13:45:00Z',
  contaId: '00000000-0000-0000-0000-0000000000aa',
  login: 'coordenacao.teste',
  acao: 'ALTERAR',
  entidade: 'conta_acesso',
  entidadeId: '00000000-0000-0000-0000-0000000000bb',
  camposAlterados: ['nome', 'perfil'],
  ip: '127.0.0.1',
};

function pagina(
  registros: readonly RegistroAuditoria[],
  total = registros.length,
): PaginaAuditoria {
  return { content: registros, page: { size: 20, number: 0, totalElements: total, totalPages: 1 } };
}

describe('ConsultaAuditoria', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsultaAuditoria],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function renderizar(resposta: PaginaAuditoria) {
    const fixture = TestBed.createComponent(ConsultaAuditoria);
    fixture.detectChanges();
    http.expectOne('/api/auditoria/entidades').flush(['conta_acesso', 'arquivo']);
    http.expectOne((r) => r.url === '/api/auditoria').flush(resposta);
    await fixture.whenStable();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('carrega a primeira página sem filtros e mostra cartões e tabela', async () => {
    const { elemento } = await renderizar(pagina([REGISTRO]));

    const cartao = elemento.querySelector('ol.cartoes li.cartao');
    expect(cartao?.textContent).toContain('Alteração');
    expect(cartao?.textContent).toContain('conta_acesso');
    expect(cartao?.textContent).toContain('coordenacao.teste');
    expect(cartao?.textContent).toContain('nome, perfil');
    expect(elemento.querySelectorAll('.tabela-rolagem tr[mat-row]').length).toBe(1);
    expect(elemento.querySelector('.total')?.textContent).toContain('1 registro');
  });

  it('envia os filtros com o período convertido para instantes do dia local', async () => {
    const { fixture, elemento } = await renderizar(pagina([]));
    const filtros = (fixture.componentInstance as unknown as { filtros: FormGroup }).filtros;
    filtros.setValue({
      inicio: new Date(2026, 10, 1),
      fim: new Date(2026, 10, 20),
      login: '  coordenacao.teste ',
      entidade: 'conta_acesso',
      acao: 'LOGIN_FALHO',
    });

    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();

    const requisicao = http.expectOne((r) => r.url === '/api/auditoria');
    const params = requisicao.request.params;
    expect(params.get('de')).toBe(new Date(2026, 10, 1).toISOString());
    expect(params.get('ate')).toBe(new Date(2026, 10, 21).toISOString());
    expect(params.get('login')).toBe('coordenacao.teste');
    expect(params.get('entidade')).toBe('conta_acesso');
    expect(params.get('acao')).toBe('LOGIN_FALHO');
    expect(params.get('page')).toBe('0');
    requisicao.flush(pagina([]));
  });

  it('orienta quando nenhum registro corresponde aos filtros', async () => {
    const { elemento } = await renderizar(pagina([]));

    expect(elemento.textContent).toContain('Nenhum registro com esses filtros');
    expect(elemento.querySelector('ol.cartoes')).toBeNull();
  });

  it('mostra erro com opção de tentar de novo', async () => {
    const fixture = TestBed.createComponent(ConsultaAuditoria);
    fixture.detectChanges();
    http.expectOne('/api/auditoria/entidades').flush([]);
    http
      .expectOne((r) => r.url === '/api/auditoria')
      .flush(null, { status: 500, statusText: 'Erro' });
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Não foi possível carregar o log de auditoria',
    );

    elemento.querySelector<HTMLButtonElement>('[role="alert"] button')?.click();
    http.expectOne((r) => r.url === '/api/auditoria').flush(pagina([REGISTRO]));
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')).toBeNull();
  });
});
