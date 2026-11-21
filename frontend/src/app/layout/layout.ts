import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterOutlet } from '@angular/router';

import { Menu } from './menu/menu';

/**
 * Layout institucional, mobile first: barra superior com logo, notificações e conta; navegação
 * principal na base da tela no celular, em trilho no tablet e em menu lateral no desktop.
 * A troca entre os três formatos é só CSS (`layout.scss` e `menu/menu.scss`).
 */
@Component({
  selector: 'app-layout',
  imports: [MatButtonModule, MatIconModule, Menu, RouterLink, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {}
