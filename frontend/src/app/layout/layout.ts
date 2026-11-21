import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { map } from 'rxjs';

import { Menu } from './menu/menu';

/** Mesmo valor de `$desktop` em `styles/_breakpoints.scss`. */
const TELA_GRANDE = '(min-width: 992px)';

/**
 * Layout institucional (inspirado no Sakai): barra superior com logo, notificações e conta;
 * menu lateral fixo em telas grandes e gaveta recolhível em telas pequenas.
 */
@Component({
  selector: 'app-layout',
  imports: [ButtonModule, Menu, RouterLink, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
  host: { '(document:keydown.escape)': 'fecharGaveta()' },
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

  protected readonly menuVisivel = computed(() =>
    this.telaGrande() ? !this.recolhidoNaTelaGrande() : this.gavetaAberta(),
  );
  protected readonly mostrarMascara = computed(() => !this.telaGrande() && this.gavetaAberta());

  protected alternarMenu(): void {
    if (this.telaGrande()) {
      this.recolhidoNaTelaGrande.update((recolhido) => !recolhido);
    } else {
      this.gavetaAberta.update((aberta) => !aberta);
    }
  }

  protected fecharGaveta(): void {
    this.gavetaAberta.set(false);
  }
}
