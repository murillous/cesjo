import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { AtendimentoApi, PessoaAtendida } from './atendimento-api';

/** Sem acento e em minúsculas, para a busca ignorar acentos. */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

/**
 * Entrada de "Escuta e PIA": a pessoa é escolhida aqui e o atendimento dela abre numa página própria
 * (ADR 0102). Por enquanto a lista traz os candidatos ativos da lista de espera.
 */
@Component({
  selector: 'app-lista-pessoas',
  imports: [MatFormFieldModule, MatIconModule, MatInputModule, MatProgressBarModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="titulo-atendimento">
      <h1 id="titulo-atendimento">Escuta e PIA</h1>
      <p class="texto-secundario orientacao">
        Escolha a pessoa para ver e registrar as escutas e o plano de atendimento.
      </p>

      <mat-form-field appearance="outline" class="busca">
        <mat-label>Buscar pelo nome</mat-label>
        <input
          matInput
          [value]="busca()"
          (input)="busca.set($any($event.target).value)"
          autocomplete="off"
        />
        <mat-icon matIconSuffix aria-hidden="true">search</mat-icon>
      </mat-form-field>

      @if (resultado().carregando) {
        <mat-progress-bar mode="indeterminate" aria-label="Carregando pessoas" />
      }
      @if (resultado().erro; as mensagem) {
        <p class="erro" role="alert">{{ mensagem }}</p>
      }
      @if (!resultado().carregando && filtradas().length === 0 && !resultado().erro) {
        <p class="texto-secundario">
          @if (busca()) {
            Nenhuma pessoa com esse nome.
          } @else {
            Nenhuma pessoa em atendimento ainda. Os candidatos da lista de espera aparecem aqui.
          }
        </p>
      }
      <ul class="pessoas" aria-label="Pessoas">
        @for (pessoa of filtradas(); track pessoa.id) {
          <li>
            <a [routerLink]="['pessoas', pessoa.id]">
              <span>{{ pessoa.nomeCompleto }}</span>
              <mat-icon aria-hidden="true">chevron_right</mat-icon>
            </a>
          </li>
        }
      </ul>
    </section>
  `,
  styles: `
    @use 'styles/breakpoints' as bp;

    h1 {
      margin: 0;
    }

    .orientacao {
      max-width: 60ch;
    }

    .busca {
      width: 100%;
    }

    .erro {
      padding: var(--sgi-space-1-5) var(--sgi-space-2);
      border-left: var(--sgi-space-0-5) solid var(--sgi-color-accent);
      background: var(--sgi-color-background-secondary);
    }

    .pessoas {
      list-style: none;
      margin: 0;
      padding: 0;
      border-top: var(--sgi-border);

      a {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: var(--sgi-space-1);
        min-height: var(--sgi-alvo-toque);
        padding: var(--sgi-space-1) var(--sgi-space-1);
        border-bottom: var(--sgi-border);
        color: inherit;
        font-weight: var(--sgi-font-weight-semibold);
        text-decoration: none;
        overflow-wrap: anywhere;

        &:hover,
        &:focus-visible {
          background: var(--sgi-color-background-secondary);
        }

        mat-icon {
          flex: none;
          color: var(--sgi-color-text-secondary);
        }
      }
    }

    @media (min-width: bp.$tablet) {
      .busca {
        max-width: 28rem;
      }
    }
  `,
})
export class ListaPessoas {
  protected readonly busca = signal('');
  protected readonly resultado = toSignal(
    inject(AtendimentoApi)
      .pessoas()
      .pipe(
        map((pessoas) => ({ pessoas, carregando: false, erro: null as string | null })),
        catchError((erro: unknown) =>
          of({
            pessoas: [] as PessoaAtendida[],
            carregando: false,
            erro: mensagemDeErro(erro, 'Não foi possível carregar as pessoas.'),
          }),
        ),
      ),
    { initialValue: { pessoas: [] as PessoaAtendida[], carregando: true, erro: null } },
  );
  protected readonly filtradas = computed(() => {
    const termo = normalizar(this.busca().trim());
    const pessoas = this.resultado().pessoas;
    return termo ? pessoas.filter((p) => normalizar(p.nomeCompleto).includes(termo)) : pessoas;
  });
}
