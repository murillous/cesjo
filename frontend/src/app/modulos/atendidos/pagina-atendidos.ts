import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Entrada do módulo Usuários atendidos. A listagem com filtros entra aqui com a issue #41. */
@Component({
  selector: 'app-pagina-atendidos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="titulo-atendidos">
      <h1 id="titulo-atendidos">Usuários atendidos</h1>
      <p class="orientacao">
        A inserção no serviço começa pelo candidato na fila da lista de espera. A lista dos usuários
        atendidos aparece aqui em breve.
      </p>
    </section>
  `,
  styles: `
    .orientacao {
      max-width: 60ch;
      color: var(--sgi-color-text-secondary);
    }
  `,
})
export class PaginaAtendidos {}
