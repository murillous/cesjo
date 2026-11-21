import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

import { MenuConta } from '../acesso/menu-conta/menu-conta';
import { Menu } from './menu/menu';

/** Mesmo valor de `$desktop` em `styles/_breakpoints.scss`. */
const TELA_GRANDE = '(min-width: 992px)';

/**
 * Layout institucional: barra superior (MatToolbar) com logo, notificações e conta, e menu
 * lateral (MatSidenav) fixo em telas grandes e gaveta em telas pequenas. A gaveta fecha com
 * Escape, com clique fora e ao escolher um item.
 */
@Component({
  selector: 'app-layout',
  imports: [
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatToolbarModule,
    Menu,
    MenuConta,
    RouterLink,
    RouterOutlet,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  protected readonly telaGrande = toSignal(
    inject(BreakpointObserver)
      .observe(TELA_GRANDE)
      .pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );

  /** Em telas grandes o menu começa aberto e pode ser recolhido. */
  private readonly recolhidoNaTelaGrande = signal(false);
  /** Em telas pequenas o menu é uma gaveta que começa fechada. */
  private readonly gavetaAberta = signal(false);

  protected readonly modo = computed(() => (this.telaGrande() ? 'side' : 'over'));
  protected readonly menuVisivel = computed(() =>
    this.telaGrande() ? !this.recolhidoNaTelaGrande() : this.gavetaAberta(),
  );

  protected alternarMenu(): void {
    if (this.telaGrande()) {
      this.recolhidoNaTelaGrande.update((recolhido) => !recolhido);
    } else {
      this.gavetaAberta.update((aberta) => !aberta);
    }
  }

  /** Sincroniza o estado quando o próprio MatSidenav fecha (Escape ou clique fora). */
  protected aoMudarAbertura(aberto: boolean): void {
    if (!this.telaGrande()) {
      this.gavetaAberta.set(aberto);
    }
  }

  protected aoNavegar(): void {
    this.gavetaAberta.set(false);
  }
}
