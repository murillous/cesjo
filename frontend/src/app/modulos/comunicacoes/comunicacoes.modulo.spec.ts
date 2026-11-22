import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { routes } from '../../app.routes';
import { Menu } from '../../layout/menu/menu';

function entrarCom(permissoes: readonly Permissao[]): void {
  TestBed.inject(Sessao).definir(contaDeTeste({ permissoes }));
}

describe('Módulo de contatos e comunicações', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes, withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
  });

  function itensDoMenu(): string[] {
    const fixture = TestBed.createComponent(Menu);
    fixture.detectChanges();
    const elemento = fixture.nativeElement as HTMLElement;
    return Array.from(elemento.querySelectorAll('a')).map((a) => a.getAttribute('href') ?? '');
  }

  it('aparece no menu só para quem tem COMUNICACOES_LER', async () => {
    entrarCom(['COMUNICACOES_LER']);
    expect(itensDoMenu()).toContain('/comunicacoes');
  });

  it('não abre para quem não tem COMUNICACOES_LER', async () => {
    entrarCom(['VISITAS_LER']);

    expect(itensDoMenu()).not.toContain('/comunicacoes');
    const harness = await RouterTestingHarness.create('/comunicacoes');
    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });

  it('lista os contatos com a pessoa ou instituição, o assunto e o tipo', async () => {
    entrarCom(['COMUNICACOES_LER', 'COMUNICACOES_REGISTRAR']);
    const controle = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create('/comunicacoes');
    controle
      .expectOne((r) => r.url === '/api/comunicacoes')
      .flush({
        content: [
          {
            id: 'c1',
            tipo: 'CONTATO_CUIDADOR_FAMILIAR',
            data: '2026-11-20',
            contraparte: 'Mãe Fictícia',
            assunto: 'Faltas da semana',
            direcao: null,
            temAnexo: false,
          },
          {
            id: 'c2',
            tipo: 'ENCAMINHAMENTO',
            direcao: 'RECEBIDA',
            data: '2026-11-19',
            contraparte: 'CRAS Fictício',
            assunto: 'Encaminhamento de usuário',
            temAnexo: true,
          },
        ],
        page: { size: 20, number: 0, totalElements: 2, totalPages: 1 },
      });
    await harness.fixture.whenStable();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Mãe Fictícia');
    expect(texto).toContain('Faltas da semana');
    expect(texto).toContain('Contato telefônico com cuidador familiar');
    expect(texto).toContain('20/11/2026');
    expect(texto).toContain('Encaminhamento recebido');
    expect(texto).toContain('com anexo');
    expect(texto).toContain('Registrar contato');
  });
});
