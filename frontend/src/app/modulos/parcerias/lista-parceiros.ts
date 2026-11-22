import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleChange, MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { Pagina } from '../contas/contas-api';
import { Parceiro, ParceriasApi } from './parcerias-api';
import { ROTULOS_TIPO_PARCEIRO } from './rotulos';

type Situacao = 'ativos' | 'inativos' | 'todos';

const SITUACOES: readonly { valor: Situacao; rotulo: string }[] = [
  { valor: 'ativos', rotulo: 'Ativos' },
  { valor: 'inativos', rotulo: 'Inativos' },
  { valor: 'todos', rotulo: 'Todos' },
];

const FILTROS: Readonly<Record<Situacao, boolean | null>> = {
  ativos: true,
  inativos: false,
  todos: null,
};

/** Lista paginada dos parceiros, com filtro por situação (RF013, ADR 0174). */
@Component({
  selector: 'app-lista-parceiros',
  imports: [
    DatePipe,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lista-parceiros.html',
  styleUrl: './lista-parceiros.scss',
})
export class ListaParceiros {
  private readonly api = inject(ParceriasApi);

  protected readonly podeGerenciar = inject(Sessao).temPermissao('PARCERIAS_GERENCIAR');
  protected readonly tipos = ROTULOS_TIPO_PARCEIRO;
  protected readonly situacoes = SITUACOES;
  protected readonly situacao = signal<Situacao>('ativos');
  protected readonly pagina = signal<Pagina<Parceiro> | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly tamanho = signal(20);

  constructor() {
    this.carregar(0);
  }

  protected filtrar(evento: MatButtonToggleChange): void {
    this.situacao.set(evento.value as Situacao);
    this.carregar(0);
  }

  protected mudarPagina(evento: PageEvent): void {
    this.tamanho.set(evento.pageSize);
    this.carregar(evento.pageIndex);
  }

  private carregar(numero: number): void {
    this.carregando.set(true);
    this.api.listar(FILTROS[this.situacao()], numero, this.tamanho()).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.erro.set(null);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar os parceiros.'));
        this.carregando.set(false);
      },
    });
  }
}
