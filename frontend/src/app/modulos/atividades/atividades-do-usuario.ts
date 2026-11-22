import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { lerDataIso } from '../visitas/datas-visita';
import { AtividadesApi, ResumoAtividade } from './atividades-api';
import { ROTULOS_TIPO_ATIVIDADE } from './rotulos';

/** Quantas atividades a ficha do usuário mostra; as demais ficam no link para a listagem filtrada. */
const NA_FICHA = 5;

/**
 * Atividades mais recentes de um usuário atendido, para a ficha dele (issue #47, ADR 0155). Só
 * título, tipo e data; a descrição fica na ficha da atividade.
 */
@Component({
  selector: 'app-atividades-do-usuario',
  imports: [DatePipe, MatButtonModule, MatIconModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="titulo-atividades-usuario">
      <h2 id="titulo-atividades-usuario">Atividades</h2>
      @if (erro()) {
        <p class="texto-secundario" role="alert">Não foi possível carregar as atividades.</p>
      } @else if (carregado() && atividades().length === 0) {
        <p class="texto-secundario">Nenhuma atividade registrada com esta pessoa.</p>
      } @else {
        <ul>
          @for (atividade of atividades(); track atividade.id) {
            <li>
              <a [routerLink]="['/atividades', atividade.id]">{{ atividade.titulo }}</a>
              <span class="texto-secundario">
                {{ tipos[atividade.tipo] }}, {{ data(atividade.data) | date: 'dd/MM/yyyy' }}
              </span>
            </li>
          }
        </ul>
        @if (total() > atividades().length) {
          <a matButton routerLink="/atividades" [queryParams]="{ pessoaId: pessoaId() }">
            Ver todas as {{ total() }} atividades
          </a>
        }
      }
    </section>
  `,
  styles: `
    // Mesmo bloco das outras seções da ficha do usuário atendido.
    section {
      padding: var(--sgi-space-2);
      border: var(--sgi-border);
      border-radius: var(--sgi-radius-md);
    }

    h2 {
      margin: 0 0 var(--sgi-space-1-5);
      font-size: var(--sgi-font-size-h3);
    }

    ul {
      margin: 0 0 var(--sgi-space-1);
      padding: 0;
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: var(--sgi-space-1);
    }

    li {
      display: flex;
      flex-direction: column;
      overflow-wrap: anywhere;
    }

    span {
      font-size: var(--sgi-font-size-small);
    }
  `,
})
export class AtividadesDoUsuario implements OnInit {
  /** `pessoaId` do usuário atendido. */
  readonly pessoaId = input.required<string>();

  private readonly api = inject(AtividadesApi);

  protected readonly tipos = ROTULOS_TIPO_ATIVIDADE;
  protected readonly data = lerDataIso;
  protected readonly atividades = signal<readonly ResumoAtividade[]>([]);
  protected readonly total = signal(0);
  protected readonly carregado = signal(false);
  protected readonly erro = signal(false);

  ngOnInit(): void {
    this.api.listar({ pessoaId: this.pessoaId() }, 0, NA_FICHA).subscribe({
      next: (pagina) => {
        this.atividades.set(pagina.content);
        this.total.set(pagina.page.totalElements);
        this.carregado.set(true);
      },
      error: () => this.erro.set(true),
    });
  }
}
