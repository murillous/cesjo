import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { ImportacaoPlanilha } from '../../compartilhado/importacao/importacao-planilha';

/**
 * Importação da lista de espera atual de planilha Excel (`/lista-espera/importar`, issue #33,
 * ADR 0219). A data de cadastro de cada linha define a posição na fila.
 */
@Component({
  selector: 'app-importar-lista-espera',
  imports: [ImportacaoPlanilha, MatButtonModule, MatIconModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="importar" aria-labelledby="titulo-importar">
      <a matButton routerLink=".." class="voltar">
        <mat-icon aria-hidden="true">arrow_back</mat-icon>
        Voltar para a fila
      </a>
      <h1 id="titulo-importar">Importar lista de espera</h1>
      <p class="texto-secundario introducao">
        Traga para o sistema os candidatos que hoje estão numa planilha. Cada um entra aguardando
        vaga, na posição que a data de cadastro da planilha dá.
      </p>
      <app-importacao-planilha
        base="/api/importacao/lista-espera"
        item="candidato"
        itens="candidatos"
      >
        <a matButton="filled" routerLink="..">Ver a fila</a>
      </app-importacao-planilha>
    </section>
  `,
  styles: `
    .importar {
      max-width: 960px;
      margin-inline: auto;
    }

    .voltar {
      margin: 0 0 var(--sgi-space-2) calc(-1 * var(--sgi-space-1));
    }

    h1 {
      margin: 0;
    }

    .introducao {
      max-width: 65ch;
      margin: var(--sgi-space-1) 0 var(--sgi-space-4);
    }
  `,
})
export class ImportarListaEspera {}
