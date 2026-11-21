import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { routes } from '../../app.routes';
import { Menu } from '../../layout/menu/menu';
import { visitaFicticia } from './visita-ficticia';

function entrarCom(permissoes: readonly Permissao[]): void {
  TestBed.inject(Sessao).definir(contaDeTeste({ permissoes }));
}

describe('Módulo de visitas', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
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

  it('aparece no menu para quem tem VISITAS_LER', () => {
    entrarCom(['VISITAS_LER']);

    expect(itensDoMenu()).toContain('/visitas');
  });

  it('some do menu e não abre a rota para quem não tem VISITAS_LER', async () => {
    entrarCom(['NOTIFICACOES_LER']);

    expect(itensDoMenu()).not.toContain('/visitas');
    const harness = await RouterTestingHarness.create('/visitas');
    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });

  it('não abre o agendamento para quem só lê', async () => {
    entrarCom(['VISITAS_LER']);

    const harness = await RouterTestingHarness.create('/visitas/nova');

    expect(harness.routeNativeElement?.textContent).not.toContain('Agendar visita');
  });

  it('mostra as visitas de hoje na página inicial', async () => {
    entrarCom(['VISITAS_LER']);
    const controle = TestBed.inject(HttpTestingController);

    const harness = await RouterTestingHarness.create('/');
    const pedido = controle.expectOne((r) => r.url === '/api/visitas');
    expect(pedido.request.params.get('de')).toBe(pedido.request.params.get('ate'));
    pedido.flush([visitaFicticia({ nomePessoa: 'Visita de Hoje' })]);
    await harness.fixture.whenStable();

    expect(harness.routeNativeElement?.textContent).toContain('Visita de Hoje');
    expect(harness.routeNativeElement?.textContent).toContain('14:30');
  });

  it('não procura visitas na página inicial de quem não as vê', async () => {
    entrarCom(['NOTIFICACOES_LER']);
    const controle = TestBed.inject(HttpTestingController);

    const harness = await RouterTestingHarness.create('/');

    controle.expectNone((r) => r.url === '/api/visitas');
    expect(harness.routeNativeElement?.textContent).toContain('Boas-vindas');
  });
});
