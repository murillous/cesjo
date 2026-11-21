import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { horaDe } from './datas-visita';
import { ROTULOS_STATUS } from './rotulos';
import { Visita } from './visitas-api';

/**
 * Visitas de um dia, em ordem de horário: a hora em destaque à esquerda, pessoa e profissionais ao
 * lado. Usado na agenda e na página inicial.
 */
@Component({
  selector: 'app-visitas-do-dia',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ul class="visitas">
      @for (visita of visitas(); track visita.id) {
        <li>
          <a
            class="visita"
            [routerLink]="['/visitas', visita.id]"
            [class.cancelada]="visita.status === 'CANCELADA'"
          >
            <time class="hora" [attr.datetime]="visita.dataHora">{{ hora(visita.dataHora) }}</time>
            <span class="pessoa">{{ visita.nomePessoa }}</span>
            <span class="profissionais">{{ nomes(visita) }}</span>
            @if (visita.status !== 'AGENDADA') {
              <span class="status">{{ rotulos[visita.status] }}</span>
            }
          </a>
        </li>
      }
    </ul>
  `,
  styles: `
    .visitas {
      list-style: none;
      margin: 0;
      padding: 0;
    }

    // A coluna da hora tem largura fixa para os horários alinharem como numa agenda de papel.
    .visita {
      display: grid;
      grid-template-columns: 4.25rem 1fr;
      column-gap: var(--sgi-space-2);
      row-gap: var(--sgi-space-0-5);
      min-height: var(--sgi-alvo-toque);
      padding: var(--sgi-space-1-5) var(--sgi-space-1);
      border-bottom: var(--sgi-border);
      color: inherit;
      text-decoration: none;

      &:hover,
      &:focus-visible {
        background: var(--sgi-color-background-secondary);
      }

      &.cancelada .hora,
      &.cancelada .pessoa {
        color: var(--sgi-color-text-secondary);
        text-decoration: line-through;
      }
    }

    .hora {
      grid-row: span 3;
      font-size: var(--sgi-font-size-h3);
      font-weight: var(--sgi-font-weight-bold);
      font-variant-numeric: tabular-nums;
      line-height: 1.2;
      color: var(--sgi-color-primary-dark);
    }

    .pessoa {
      font-weight: var(--sgi-font-weight-semibold);
      overflow-wrap: anywhere;
    }

    .profissionais,
    .status {
      font-size: var(--sgi-font-size-small);
      color: var(--sgi-color-text-secondary);
    }

    .status {
      font-weight: var(--sgi-font-weight-medium);
    }
  `,
})
export class VisitasDoDia {
  readonly visitas = input.required<readonly Visita[]>();

  protected readonly rotulos = ROTULOS_STATUS;
  protected readonly hora = horaDe;

  protected nomes(visita: Visita): string {
    return visita.profissionais.map((p) => p.nome).join(', ');
  }
}
