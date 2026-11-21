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
import { Colaborador, ColaboradoresApi, FiltroSituacao } from './colaboradores-api';
import { ROTULOS_FUNCAO } from './rotulos';

/** Situações oferecidas no filtro. O valor de "Todos" vai como texto porque o toggle não aceita `null`. */
const SITUACOES: readonly { valor: 'ativos' | 'inativos' | 'todos'; rotulo: string }[] = [
  { valor: 'ativos', rotulo: 'Ativos' },
  { valor: 'inativos', rotulo: 'Inativos' },
  { valor: 'todos', rotulo: 'Todos' },
];

/** Lista paginada dos colaboradores, com filtro por situação (issue #50). */
@Component({
  selector: 'app-lista-colaboradores',
  imports: [
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lista-colaboradores.html',
  styleUrl: './lista-colaboradores.scss',
})
export class ListaColaboradores {
  private readonly api = inject(ColaboradoresApi);

  protected readonly podeGerenciar = inject(Sessao).temPermissao('COLABORADORES_GERENCIAR');
  protected readonly funcoes = ROTULOS_FUNCAO;
  protected readonly situacoes = SITUACOES;
  protected readonly situacao = signal<'ativos' | 'inativos' | 'todos'>('ativos');
  protected readonly pagina = signal<Pagina<Colaborador> | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly tamanho = signal(20);

  constructor() {
    this.carregar(0);
  }

  protected filtrar(evento: MatButtonToggleChange): void {
    this.situacao.set(evento.value as 'ativos' | 'inativos' | 'todos');
    this.carregar(0);
  }

  protected mudarPagina(evento: PageEvent): void {
    this.tamanho.set(evento.pageSize);
    this.carregar(evento.pageIndex);
  }

  private carregar(numero: number): void {
    const filtros: Record<string, FiltroSituacao> = { ativos: true, inativos: false, todos: null };
    this.carregando.set(true);
    this.api.listar(filtros[this.situacao()], numero, this.tamanho()).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.erro.set(null);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar os colaboradores.'));
        this.carregando.set(false);
      },
    });
  }
}
