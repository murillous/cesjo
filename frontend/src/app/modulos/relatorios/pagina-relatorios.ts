import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ABAS } from './abas';

/** Casca da área de relatórios: título, abas e o painel da aba aberta (ADR 0197). */
@Component({
  selector: 'app-pagina-relatorios',
  imports: [MatTabsModule, RouterLink, RouterLinkActive, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="relatorios" aria-labelledby="titulo-relatorios">
      <h1 id="titulo-relatorios">Relatórios</h1>
      <nav mat-tab-nav-bar [tabPanel]="painel" aria-label="Relatórios disponíveis">
        @for (aba of abas; track aba.caminho) {
          <a
            mat-tab-link
            [routerLink]="aba.caminho"
            routerLinkActive
            #ativa="routerLinkActive"
            [active]="ativa.isActive"
            >{{ aba.rotulo }}</a
          >
        }
      </nav>
      <mat-tab-nav-panel #painel>
        <router-outlet />
      </mat-tab-nav-panel>
    </section>
  `,
  styles: `
    mat-tab-nav-panel {
      display: block;
      padding-top: var(--sgi-space-3);
    }
  `,
})
export class PaginaRelatorios {
  protected readonly abas = ABAS;
}
