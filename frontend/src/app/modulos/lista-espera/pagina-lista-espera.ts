import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { Sessao } from '../../acesso/sessao';

/**
 * Entrada do módulo Lista de espera. A fila com posição, filtros e busca entra aqui com a issue #28.
 */
@Component({
  selector: 'app-pagina-lista-espera',
  imports: [MatButtonModule, MatIconModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="titulo-lista-espera">
      <header class="cabecalho">
        <h1 id="titulo-lista-espera">Lista de espera</h1>
        @if (podeCadastrar) {
          <a matButton="filled" routerLink="nova">
            <mat-icon aria-hidden="true">person_add</mat-icon>
            Cadastrar candidato
          </a>
        }
      </header>
      <p class="orientacao">A fila com a posição de cada candidato aparece aqui em breve.</p>
    </section>
  `,
  styles: `
    @use 'styles/breakpoints' as bp;

    .cabecalho {
      display: flex;
      flex-direction: column;
      gap: var(--sgi-space-2);
      margin-bottom: var(--sgi-space-2);

      h1 {
        margin: 0;
      }

      a {
        min-height: var(--sgi-alvo-toque);
      }
    }

    .orientacao {
      max-width: 60ch;
      color: var(--sgi-color-text-secondary);
    }

    @media (min-width: bp.$tablet) {
      .cabecalho {
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
      }
    }
  `,
})
export class PaginaListaEspera {
  protected readonly podeCadastrar = inject(Sessao).temPermissao('LISTA_ESPERA_CADASTRAR');
}
