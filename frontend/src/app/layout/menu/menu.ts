import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { Sessao } from '../../acesso/sessao';
import { MODULOS } from '../../modulos/registro';

interface EntradaMenu {
  rotulo: string;
  icone: string;
  link: string;
}

/**
 * Menu lateral montado a partir dos módulos registrados em `modulos/registro.ts`. Módulos que
 * exigem uma permissão só aparecem para quem a tem.
 */
@Component({
  selector: 'app-menu',
  imports: [MatIconModule, MatListModule, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './menu.html',
  styleUrl: './menu.scss',
})
export class Menu {
  /** Emitido ao escolher um item, para o layout fechar a gaveta no celular. */
  readonly navegou = output();

  private readonly sessao = inject(Sessao);

  protected readonly entradas = computed<readonly EntradaMenu[]>(() =>
    MODULOS.filter((modulo) => !modulo.permissao || this.sessao.temPermissao(modulo.permissao))
      .flatMap((modulo) => (modulo.menu ? [{ ...modulo.menu, link: `/${modulo.caminho}` }] : []))
      .sort((a, b) => a.ordem - b.ordem)
      .map(({ rotulo, icone, link }) => ({ rotulo, icone, link })),
  );
}
