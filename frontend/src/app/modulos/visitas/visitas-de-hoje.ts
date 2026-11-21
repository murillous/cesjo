import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { dataIso } from './datas-visita';
import { Visita, VisitasApi } from './visitas-api';
import { VisitasDoDia } from './visitas-do-dia';

/** Agenda do dia na página inicial "Hoje": as visitas domiciliares de hoje, em ordem de horário. */
@Component({
  selector: 'app-visitas-de-hoje',
  imports: [MatButtonModule, RouterLink, VisitasDoDia],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="hoje" aria-labelledby="titulo-visitas-hoje">
      <header>
        <h2 id="titulo-visitas-hoje">Visitas domiciliares</h2>
        <a matButton routerLink="/visitas">Ver agenda</a>
      </header>
      @if (erro(); as mensagem) {
        <p class="erro" role="alert">{{ mensagem }}</p>
      } @else if (visitas(); as lista) {
        @if (lista.length) {
          <app-visitas-do-dia [visitas]="lista" />
        } @else {
          <p class="vazio">
            Nenhuma visita hoje.
            @if (podeAgendar) {
              <a routerLink="/visitas/nova">Agendar uma visita</a>
            }
          </p>
        }
      }
    </section>
  `,
  styles: `
    header {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: var(--sgi-space-1);
      padding-bottom: var(--sgi-space-1);
      border-bottom: 2px solid var(--sgi-color-primary-dark);

      h2 {
        margin: 0;
        font-size: var(--sgi-font-size-h3);
      }
    }

    .vazio {
      color: var(--sgi-color-text-secondary);
    }

    .erro {
      padding: var(--sgi-space-1-5) var(--sgi-space-2);
      border-left: var(--sgi-space-0-5) solid var(--sgi-color-accent);
      background: var(--sgi-color-background-secondary);
    }
  `,
})
export class VisitasDeHoje {
  protected readonly podeAgendar = inject(Sessao).temPermissao('VISITAS_AGENDAR');
  protected readonly visitas = signal<Visita[] | null>(null);
  protected readonly erro = signal<string | null>(null);

  constructor() {
    const hoje = dataIso(new Date());
    inject(VisitasApi)
      .agenda(hoje, hoje)
      .subscribe({
        next: (visitas) => this.visitas.set(visitas),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar as visitas de hoje.')),
      });
  }
}
