import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

import { MenuConta } from '../acesso/menu-conta/menu-conta';
import { SinoNotificacoes } from '../notificacoes/sino-notificacoes';
import { Menu } from './menu/menu';

/**
 * Layout institucional, mobile first: barra superior com logo, notificações e conta; navegação
 * principal na base da tela no celular, em trilho no tablet e em menu lateral no desktop.
 * A troca entre os três formatos é só CSS (`layout.scss` e `menu/menu.scss`).
 *
 * As barras são sticky e o conteúdo rola por baixo delas. O Chrome não rola um campo que recebe
 * foco se ele já está dentro da janela, mesmo coberto pela barra; `focusin` pede ao navegador a
 * rolagem mínima que respeita o `scroll-padding` de `styles.scss` (WCAG 2.4.11, ADR 0124). A
 * rolagem espera o próximo quadro: sair de um campo obrigatório vazio mostra a mensagem de erro dele
 * e empurra o resto do formulário para baixo.
 */
@Component({
  selector: 'app-layout',
  imports: [Menu, MenuConta, RouterLink, RouterOutlet, SinoNotificacoes],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
  host: { '(focusin)': 'manterFocoVisivel($event)' },
})
export class Layout {
  protected manterFocoVisivel(evento: FocusEvent): void {
    const alvo = evento.target;
    if (alvo instanceof HTMLElement) {
      requestAnimationFrame(() => alvo.scrollIntoView({ block: 'nearest', inline: 'nearest' }));
    }
  }
}
