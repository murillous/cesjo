import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter, Router, Routes } from '@angular/router';

import { ModuloFrontend } from '../modulos/modulo';
import { MODULOS_FRONTEND } from '../modulos/registro';
import { Layout } from './layout';

@Component({ template: '' })
class PaginaVazia {}

function modulo(
  caminho: string,
  rotulo: string,
  ordem: number,
  rotuloCurto?: string,
): ModuloFrontend {
  return {
    caminho,
    rotas: () => Promise.resolve([{ path: '', component: PaginaVazia }] satisfies Routes),
    menu: { rotulo, icone: 'circle', ordem, ...(rotuloCurto ? { rotuloCurto } : {}) },
  };
}

const SETE_MODULOS: readonly ModuloFrontend[] = [
  modulo('', 'Início', 0),
  modulo('lista-espera', 'Lista de espera', 10, 'Fila'),
  modulo('atendimento', 'Atendimento', 20),
  modulo('atendidos', 'Atendidos', 30),
  modulo('atividades', 'Atividades', 40),
  modulo('institucional', 'Institucional', 50),
  modulo('relatorios', 'Relatórios', 60),
];

describe('Layout', () => {
  async function renderizar(modulos?: readonly ModuloFrontend[]) {
    const lista = modulos ?? [modulo('', 'Início', 0)];
    await TestBed.configureTestingModule({
      imports: [Layout],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(lista.map((m) => ({ path: m.caminho, loadChildren: m.rotas }))),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
        ...(modulos ? [{ provide: MODULOS_FRONTEND, useValue: modulos }] : []),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(Layout);
    await fixture.whenStable();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  function rotulos(elementos: Iterable<Element>): string[] {
    return Array.from(
      elementos,
      (item) => item.querySelector('.rotulo-completo')?.textContent?.trim() ?? '',
    );
  }

  it('exibe o logo institucional na barra superior', async () => {
    const { elemento } = await renderizar();
    const logo = elemento.querySelector<HTMLImageElement>('header.topbar img.logo');

    expect(logo).not.toBeNull();
    expect(logo?.getAttribute('src')).toBe('logo-pobres-servos-cesjo.png');
    expect(logo?.alt).toContain('CESJO');
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

  it('exibe a navegação principal e marca o item da página atual', async () => {
    const { fixture, elemento } = await renderizar();
    await TestBed.inject(Router).navigateByUrl('/');
    await fixture.whenStable();
    const menu = elemento.querySelector('nav[aria-label="Menu principal"]');
    const itens = Array.from(menu?.querySelectorAll('a.item') ?? []);

    expect(rotulos(itens)).toEqual(['Início']);
    expect(itens[0]?.getAttribute('href')).toBe('/');
    expect(itens[0]?.classList).toContain('ativo');
    expect(itens[0]?.getAttribute('aria-current')).toBe('page');
  });

  it('esconde do menu o módulo que exige permissão que a conta não tem', async () => {
    const { elemento } = await renderizar([
      modulo('', 'Início', 0),
      { ...modulo('contas', 'Contas', 10), permissao: 'CONTAS_GERENCIAR' },
    ]);

    expect(rotulos(elemento.querySelectorAll('a.item'))).toEqual(['Início']);
  });

  it('não mostra "Mais" quando todos os módulos cabem na barra inferior', async () => {
    const { elemento } = await renderizar(SETE_MODULOS.slice(0, 5));

    expect(elemento.querySelector('.so-na-barra')).toBeNull();
    expect(elemento.querySelectorAll('.fora-da-barra')).toHaveLength(0);
  });

  it('com mais de cinco módulos, deixa quatro na barra e manda o resto para "Mais"', async () => {
    const { elemento } = await renderizar(SETE_MODULOS);
    const naBarra = elemento.querySelectorAll('.posicao:not(.fora-da-barra):not(.so-na-barra) a');
    const foraDaBarra = elemento.querySelectorAll('.fora-da-barra a');

    expect(rotulos(naBarra)).toEqual(['Início', 'Lista de espera', 'Atendimento', 'Atendidos']);
    expect(rotulos(foraDaBarra)).toEqual(['Atividades', 'Institucional', 'Relatórios']);
    expect(elemento.querySelector('.so-na-barra .rotulo')?.textContent?.trim()).toBe('Mais');
  });

  it('usa o rótulo curto na barra inferior e os dois rótulos no nome acessível', async () => {
    const { elemento } = await renderizar(SETE_MODULOS);
    const item = elemento.querySelectorAll('a.item')[1];

    expect(item?.querySelector('.rotulo-curto')?.textContent?.trim()).toBe('Fila');
    expect(item?.querySelector('.rotulo-completo')?.textContent?.trim()).toBe('Lista de espera');
    expect(item?.getAttribute('aria-label')).toBe('Fila, Lista de espera');
  });

  it('"Mais" abre um bottom sheet com os módulos que não couberam', async () => {
    const { fixture, elemento } = await renderizar(SETE_MODULOS);

    elemento.querySelector<HTMLButtonElement>('.so-na-barra button')?.click();
    await fixture.whenStable();

    const folha = document.querySelector('app-menu-mais nav[aria-label="Mais módulos"]');
    expect(Array.from(folha?.querySelectorAll('a') ?? [], (a) => a.textContent?.trim())).toEqual([
      'circle Atividades',
      'circle Institucional',
      'circle Relatórios',
    ]);
  });

  it('marca "Mais" como ativo quando a página atual está dentro dele', async () => {
    const { fixture, elemento } = await renderizar(SETE_MODULOS);
    const mais = elemento.querySelector('.so-na-barra button');

    expect(mais?.classList).not.toContain('ativo');

    await TestBed.inject(Router).navigateByUrl('/relatorios');
    await fixture.whenStable();

    expect(mais?.classList).toContain('ativo');
  });
});
