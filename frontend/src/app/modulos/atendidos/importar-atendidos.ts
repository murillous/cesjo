import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { ImportacaoPlanilha } from '../../compartilhado/importacao/importacao-planilha';

/**
 * Importação dos usuários atendidos atuais de planilha Excel (`/atendidos/importar`, issue #62,
 * ADR 0221). Mesmo fluxo da lista de espera (ADR 0219): cada linha válida vira pessoa, inscrição e
 * inserção ativa no serviço.
 */
@Component({
  selector: 'app-importar-atendidos',
  imports: [ImportacaoPlanilha, MatButtonModule, MatIconModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="importar" aria-labelledby="titulo-importar">
      <a matButton routerLink=".." class="voltar">
        <mat-icon aria-hidden="true">arrow_back</mat-icon>
        Voltar para os usuários atendidos
      </a>
      <h1 id="titulo-importar">Importar usuários atendidos</h1>
      <p class="texto-secundario introducao">
        Traga para o sistema quem já frequenta o Centro-Dia. Cada pessoa entra como usuária ativa,
        no turno e com a data de inserção da planilha, e o responsável vira o cuidador familiar.
        Quem ainda espera vaga entra pela importação da lista de espera.
      </p>
      <app-importacao-planilha
        base="/api/importacao/atendidos"
        item="usuário atendido"
        itens="usuários atendidos"
      >
        <a matButton="filled" routerLink="..">Ver os usuários atendidos</a>
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
export class ImportarAtendidos {}
