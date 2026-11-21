import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { Layout } from './layout';

describe('Layout', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Layout],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
  });

  async function renderizar() {
    const fixture = TestBed.createComponent(Layout);
    await fixture.whenStable();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('exibe o logo institucional na barra superior', async () => {
    const { elemento } = await renderizar();
    const logo = elemento.querySelector<HTMLImageElement>('header mat-toolbar img.logo');

    expect(logo).not.toBeNull();
    expect(logo?.getAttribute('src')).toBe('logo-pobres-servos-cesjo.png');
    expect(logo?.alt).toContain('CESJO');
  });

  it('exibe o menu lateral com o item da página inicial', async () => {
    const { elemento } = await renderizar();
    const menu = elemento.querySelector('mat-sidenav nav[aria-label="Menu principal"]');
    const itens = Array.from(menu?.querySelectorAll('a') ?? []);

    expect(menu).not.toBeNull();
    expect(itens.map((item) => item.textContent?.replace('home', '').trim())).toContain('Início');
    expect(itens[0]?.getAttribute('href')).toBe('/');
  });

  it('reserva espaço para a conta', async () => {
    const { elemento } = await renderizar();
    const acoes = elemento.querySelector('.acoes');

    expect(acoes?.querySelector('[aria-label="Minha conta"]')).not.toBeNull();
  });

  it('esconde o sino de notificações sem sessão', async () => {
    const { elemento } = await renderizar();

    expect(elemento.querySelector('.acoes app-sino-notificacoes')).not.toBeNull();
    expect(elemento.querySelector('.acoes button.sino')).toBeNull();
  });

  it('usa gaveta (modo over) em telas pequenas e abre pelo botão', async () => {
    const { fixture, elemento } = await renderizar();
    const sidenav = elemento.querySelector('mat-sidenav');
    const botao = elemento.querySelector<HTMLButtonElement>('button.alternar-menu');

    expect(sidenav?.classList).toContain('mat-drawer-over');
    expect(sidenav?.classList).not.toContain('mat-drawer-opened');
    expect(botao?.getAttribute('aria-expanded')).toBe('false');

    botao?.click();
    await fixture.whenStable();

    expect(sidenav?.classList).toContain('mat-drawer-opened');
    expect(botao?.getAttribute('aria-expanded')).toBe('true');
    expect(botao?.getAttribute('aria-label')).toBe('Fechar menu');
  });

  it('fecha a gaveta com clique fora', async () => {
    const { fixture, elemento } = await renderizar();
    elemento.querySelector<HTMLButtonElement>('button.alternar-menu')?.click();
    await fixture.whenStable();

    elemento.querySelector<HTMLElement>('.mat-drawer-backdrop')?.click();
    await fixture.whenStable();

    expect(elemento.querySelector('mat-sidenav')?.classList).not.toContain('mat-drawer-opened');
    expect(elemento.querySelector('button.alternar-menu')?.getAttribute('aria-expanded')).toBe(
      'false',
    );
  });

  it('fecha a gaveta com Escape', async () => {
    const { fixture, elemento } = await renderizar();
    elemento.querySelector<HTMLButtonElement>('button.alternar-menu')?.click();
    await fixture.whenStable();

    elemento
      .querySelector('mat-sidenav')
      ?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', keyCode: 27, bubbles: true }));
    await fixture.whenStable();

    expect(elemento.querySelector('mat-sidenav')?.classList).not.toContain('mat-drawer-opened');
  });
});
