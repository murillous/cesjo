import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { providePrimeNG } from 'primeng/config';

import { Layout } from './layout';

describe('Layout', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Layout],
      providers: [provideRouter([]), providePrimeNG({ theme: 'none' })],
    }).compileComponents();
  });

  function renderizar(): HTMLElement {
    const fixture = TestBed.createComponent(Layout);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('exibe o logo institucional na barra superior', () => {
    const logo = renderizar().querySelector<HTMLImageElement>('header img.logo');

    expect(logo).not.toBeNull();
    expect(logo?.getAttribute('src')).toBe('logo-pobres-servos-cesjo.png');
    expect(logo?.alt).toContain('CESJO');
  });

  it('exibe o menu lateral com o item da página inicial', () => {
    const elemento = renderizar();
    const menu = elemento.querySelector('nav[aria-label="Menu principal"]');
    const itens = Array.from(menu?.querySelectorAll('a') ?? []);

    expect(menu).not.toBeNull();
    expect(itens.map((item) => item.textContent?.trim())).toContain('Início');
    expect(itens[0]?.getAttribute('href')).toBe('/');
  });

  it('reserva espaço para notificações e conta', () => {
    const acoes = renderizar().querySelector('.acoes');

    expect(acoes?.querySelector('[aria-label="Notificações"]')).not.toBeNull();
    expect(acoes?.querySelector('[aria-label="Minha conta"]')).not.toBeNull();
  });

  it('abre e fecha a gaveta do menu em telas pequenas', () => {
    const fixture = TestBed.createComponent(Layout);
    fixture.detectChanges();
    const elemento = fixture.nativeElement as HTMLElement;
    const layout = elemento.querySelector('.layout');
    const botao = elemento.querySelector<HTMLButtonElement>('header button');

    expect(layout?.classList).toContain('menu-oculto');

    botao?.click();
    fixture.detectChanges();
    expect(layout?.classList).not.toContain('menu-oculto');
    expect(elemento.querySelector('.mascara')).not.toBeNull();

    elemento.querySelector<HTMLElement>('.mascara')?.click();
    fixture.detectChanges();
    expect(layout?.classList).toContain('menu-oculto');
  });
});
