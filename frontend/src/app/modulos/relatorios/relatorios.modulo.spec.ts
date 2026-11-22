import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { routes } from '../../app.routes';
import { Menu } from '../../layout/menu/menu';

function entrarCom(permissoes: readonly Permissao[]): void {
  TestBed.inject(Sessao).definir({
    id: '00000000-0000-0000-0000-000000000001',
    login: 'conta.teste',
    nome: 'Conta de teste',
    perfil: permissoes.includes('RELATORIOS_LER') ? 'COORDENADOR' : 'CONSULTA',
    permissoes,
    trocaSenhaObrigatoria: false,
  });
}

describe('Módulo de relatórios', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
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

  it('aparece no menu para quem tem RELATORIOS_LER', () => {
    entrarCom(['RELATORIOS_LER']);

    expect(itensDoMenu()).toContain('/relatorios');
  });

  it('some do menu para quem não tem RELATORIOS_LER', () => {
    entrarCom(['NOTIFICACOES_LER']);

    expect(itensDoMenu()).not.toContain('/relatorios');
  });

  it('não abre a rota para quem não tem RELATORIOS_LER', async () => {
    entrarCom(['NOTIFICACOES_LER']);

    const harness = await RouterTestingHarness.create('/relatorios');

    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });

  it('abre os relatórios para quem tem RELATORIOS_LER', async () => {
    entrarCom(['RELATORIOS_LER']);

    const harness = await RouterTestingHarness.create('/relatorios');

    expect(harness.routeNativeElement?.textContent).toContain('Perfil dos atendidos');
  });
});
