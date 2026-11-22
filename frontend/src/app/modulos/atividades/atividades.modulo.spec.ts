import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
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

describe('Módulo de atividades', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes, withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
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

  it('aparece no menu só para quem tem ATIVIDADES_LER', () => {
    entrarCom(['ATIVIDADES_LER']);
    expect(itensDoMenu()).toContain('/atividades');
  });

  it('não abre para quem não tem ATIVIDADES_LER', async () => {
    entrarCom(['VISITAS_LER']);

    expect(itensDoMenu()).not.toContain('/atividades');
    const harness = await RouterTestingHarness.create('/atividades');
    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });

  it('lista as atividades com a data, o tipo e os participantes', async () => {
    entrarCom(['ATIVIDADES_LER', 'ATIVIDADES_REGISTRAR']);
    const controle = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create('/atividades');
    controle
      .expectOne((r) => r.url === '/api/atividades')
      .flush({
        content: [
          {
            id: 'a1',
            tipo: 'COM_FAMILIAS',
            data: '2026-11-20',
            titulo: 'Roda de conversa',
            participantes: [
              { pessoaId: 'p1', nomeCompleto: 'Ana Fictícia' },
              { pessoaId: 'p2', nomeCompleto: 'Bruno Fictício' },
              { pessoaId: 'p3', nomeCompleto: 'Carla Fictícia' },
            ],
            profissionais: [],
          },
        ],
        page: { size: 20, number: 0, totalElements: 1, totalPages: 1 },
      });
    await harness.fixture.whenStable();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Roda de conversa');
    expect(texto).toContain('Atividade com famílias');
    expect(texto).toContain('Ana, Bruno e mais 1');
    expect(texto).toContain('Registrar atividade');
  });

  it('sem ATIVIDADES_REGISTRAR, não oferece o registro', async () => {
    entrarCom(['ATIVIDADES_LER']);
    const controle = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create('/atividades');
    controle
      .expectOne((r) => r.url === '/api/atividades')
      .flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
    await harness.fixture.whenStable();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Nenhuma atividade registrada');
    expect(texto).not.toContain('Registrar');
  });
});
