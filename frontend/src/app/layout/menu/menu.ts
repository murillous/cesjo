import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { MatIconModule } from '@angular/material/icon';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, map } from 'rxjs';

import { Sessao } from '../../acesso/sessao';
import { MODULOS_FRONTEND } from '../../modulos/registro';
import { MenuMais } from '../menu-mais/menu-mais';
import { EntradaMenu } from './entrada-menu';

/** Quantos itens cabem na barra inferior do celular, contando o "Mais". */
export const ITENS_NA_BARRA_INFERIOR = 5;

/**
 * Navegação principal, montada a partir dos módulos registrados. Módulos que exigem uma permissão
 * só aparecem para quem a tem.
 *
 * Mobile first: no celular é a barra inferior, com até cinco destinos. Se houver mais módulos,
 * os quatro primeiros ficam na barra e o resto vai para "Mais", que abre um bottom sheet.
 * A partir do tablet vira trilho lateral e, no desktop, menu lateral com rótulos; nesses casos
 * todos os itens aparecem e o "Mais" some (só CSS, em `menu.scss`).
 */
@Component({
  selector: 'app-menu',
  imports: [MatIconModule, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './menu.html',
  styleUrl: './menu.scss',
})
export class Menu {
  private readonly router = inject(Router);
  private readonly bottomSheet = inject(MatBottomSheet);
  private readonly sessao = inject(Sessao);
  private readonly modulos = inject(MODULOS_FRONTEND);

  /** Emitido ao escolher um item. */
  readonly navegou = output();

  protected readonly entradas = computed<readonly EntradaMenu[]>(() =>
    this.modulos
      .filter((modulo) => !modulo.permissao || this.sessao.temPermissao(modulo.permissao))
      .flatMap((modulo) => (modulo.menu ? [{ ...modulo.menu, link: `/${modulo.caminho}` }] : []))
      .sort((a, b) => a.ordem - b.ordem)
      .map(({ rotulo, rotuloCurto, icone, link }) => ({
        rotulo,
        rotuloCurto: rotuloCurto ?? rotulo,
        icone,
        link,
      })),
  );

  /** Índice a partir do qual os itens saem da barra inferior e vão para "Mais". */
  protected readonly limiteBarra = computed(() =>
    this.entradas().length > ITENS_NA_BARRA_INFERIOR
      ? ITENS_NA_BARRA_INFERIOR - 1
      : ITENS_NA_BARRA_INFERIOR,
  );

  protected readonly demais = computed(() => this.entradas().slice(this.limiteBarra()));

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  /** "Mais" fica marcado quando a página atual é de um módulo que está dentro dele. */
  protected readonly maisAtivo = computed(() => {
    this.url();
    return this.demais().some((entrada) =>
      this.router.isActive(entrada.link, {
        paths: entrada.link === '/' ? 'exact' : 'subset',
        queryParams: 'ignored',
        fragment: 'ignored',
        matrixParams: 'ignored',
      }),
    );
  });

  protected exato(entrada: EntradaMenu): boolean {
    return entrada.link === '/';
  }

  protected abrirMais(): void {
    const referencia = this.bottomSheet.open(MenuMais, {
      data: this.demais(),
      ariaLabel: 'Mais módulos',
    });
    referencia.afterDismissed().subscribe((navegou) => {
      if (navegou) {
        this.navegou.emit();
      }
    });
  }
}
