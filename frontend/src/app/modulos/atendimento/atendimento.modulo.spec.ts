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

describe('Módulo de escuta e PIA', () => {
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

  it('aparece no menu só para quem tem ESCUTA_LER', () => {
    entrarCom(['ESCUTA_LER']);
    expect(itensDoMenu()).toContain('/atendimento');
  });

  it('não abre para quem não tem ESCUTA_LER', async () => {
    entrarCom(['VISITAS_LER', 'VISITAS_AGENDAR']);

    expect(itensDoMenu()).not.toContain('/atendimento');
    const harness = await RouterTestingHarness.create('/atendimento');
    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });

  it('lista as pessoas e filtra pelo nome sem acento', async () => {
    entrarCom(['ESCUTA_LER']);
    const controle = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create('/atendimento');
    controle.expectOne('/api/atendimento/pessoas').flush([
      { id: 'p1', nomeCompleto: 'Ana Fictícia' },
      { id: 'p2', nomeCompleto: 'José Fictício' },
    ]);
    await harness.fixture.whenStable();
    expect(harness.routeNativeElement?.querySelectorAll('.pessoas li')).toHaveLength(2);

    const busca = harness.routeNativeElement!.querySelector('input')!;
    busca.value = 'jose';
    busca.dispatchEvent(new Event('input'));
    await harness.fixture.whenStable();

    expect(harness.routeNativeElement?.querySelectorAll('.pessoas li')).toHaveLength(1);
  });

  it('mostra as escutas da pessoa sem os textos', async () => {
    entrarCom(['ESCUTA_LER', 'ESCUTA_REGISTRAR']);
    const controle = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create('/atendimento/pessoas/p1');
    controle.expectOne('/api/atendimento/pessoas/p1').flush({ id: 'p1', nomeCompleto: 'Ana' });
    controle
      .expectOne((r) => r.url === '/api/escutas')
      .flush([
        {
          id: 'e1',
          pessoaId: 'p1',
          dataEscuta: '2026-11-20',
          profissional: { id: 'c1', nome: 'Psicóloga Fictícia', funcao: 'PSICOLOGO' },
        },
      ]);
    await harness.fixture.whenStable();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Psicóloga Fictícia');
    expect(texto).toContain('Registrar escuta');
  });
});
