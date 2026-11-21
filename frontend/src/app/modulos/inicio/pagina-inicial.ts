import { DatePipe, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { Sessao } from '../../acesso/sessao';
import { VisitasDeHoje } from '../visitas/visitas-de-hoje';

/**
 * Página inicial "Hoje". O número do dia dentro do disco amarelo repete o indicador de item
 * ativo da navegação. Abaixo, a agenda do dia: as visitas domiciliares de hoje, para quem vê
 * visitas (ADR 0072).
 */
@Component({
  selector: 'app-pagina-inicial',
  imports: [DatePipe, TitleCasePipe, VisitasDeHoje],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="hoje" aria-labelledby="titulo-hoje">
      <header class="cabecalho">
        <span class="dia" aria-hidden="true">{{ hoje | date: 'd' }}</span>
        <div>
          <h1 id="titulo-hoje">{{ hoje | date: 'EEEE' | titlecase }}</h1>
          <p class="data">{{ hoje | date: "d 'de' MMMM 'de' y" }}</p>
        </div>
      </header>
      @if (veVisitas) {
        <app-visitas-de-hoje />
      } @else {
        <p class="orientacao">
          Boas-vindas ao Sistema de Gestão do Centro-Dia. Escolha um módulo no menu para começar.
        </p>
      }
    </section>
  `,
  styles: `
    @use 'styles/breakpoints' as bp;

    .cabecalho {
      display: flex;
      align-items: center;
      gap: var(--sgi-space-2);
      margin-bottom: var(--sgi-space-3);
    }

    .dia {
      display: grid;
      place-items: center;
      flex: none;
      width: 4.5rem;
      height: 4.5rem;
      border-radius: 50%;
      background: var(--sgi-color-accent);
      color: var(--sgi-color-primary-dark);
      font-size: 2.25rem;
      font-weight: var(--sgi-font-weight-bold);
      font-variant-numeric: tabular-nums;
      line-height: 1;
    }

    h1 {
      margin: 0;
    }

    .data {
      margin: 0;
      color: var(--sgi-color-text-secondary);
    }

    .orientacao {
      max-width: 60ch;
    }

    @media (min-width: bp.$desktop) {
      .dia {
        width: 6rem;
        height: 6rem;
        font-size: 3rem;
      }
    }
  `,
})
export class PaginaInicial {
  protected readonly hoje = new Date();
  protected readonly veVisitas = inject(Sessao).temPermissao('VISITAS_LER');
}
