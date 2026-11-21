import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_BOTTOM_SHEET_DATA, MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { EntradaMenu } from '../menu/entrada-menu';

/**
 * Bottom sheet do "Mais" na barra inferior do celular: lista os módulos que não couberam na barra.
 * Fecha com `true` quando a pessoa escolhe um módulo.
 */
@Component({
  selector: 'app-menu-mais',
  imports: [MatIconModule, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav aria-label="Mais módulos">
      <ul class="lista">
        @for (entrada of entradas; track entrada.link) {
          <li>
            <a
              class="item"
              [routerLink]="entrada.link"
              routerLinkActive="ativo"
              ariaCurrentWhenActive="page"
              (click)="fechar()"
            >
              <span class="indicador"
                ><mat-icon aria-hidden="true">{{ entrada.icone }}</mat-icon></span
              >
              {{ entrada.rotulo }}
            </a>
          </li>
        }
      </ul>
    </nav>
  `,
  styles: `
    .lista {
      margin: 0;
      padding: var(--sgi-space-1) 0 calc(var(--sgi-space-2) + env(safe-area-inset-bottom));
      list-style: none;
    }

    .item {
      display: flex;
      align-items: center;
      gap: var(--sgi-space-1-5);
      min-height: var(--sgi-alvo-toque);
      padding: var(--sgi-space-0-5) var(--sgi-space-1);
      border-radius: var(--sgi-radius-md);
      color: var(--sgi-color-text-primary);
      text-decoration: none;
    }

    .indicador {
      display: grid;
      place-items: center;
      width: var(--sgi-indicador-ativo);
      height: var(--sgi-indicador-ativo);
      border-radius: 50%;
    }

    .item.ativo {
      font-weight: var(--sgi-font-weight-semibold);

      .indicador {
        background: var(--sgi-color-accent);
      }
    }
  `,
})
export class MenuMais {
  private readonly referencia = inject<MatBottomSheetRef<MenuMais, boolean>>(MatBottomSheetRef);

  protected readonly entradas = inject<readonly EntradaMenu[]>(MAT_BOTTOM_SHEET_DATA);

  protected fechar(): void {
    this.referencia.dismiss(true);
  }
}
