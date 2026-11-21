import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { ROTULOS_PERFIL } from '../../acesso/perfis';
import { mensagemDeErro } from '../../acesso/problema';
import { Conta, ContasApi, Pagina } from './contas-api';
import { situacaoConta } from './situacao-conta';

/** Lista paginada das contas de acesso, para o Coordenador (issue #15). */
@Component({
  selector: 'app-lista-contas',
  imports: [MatButtonModule, MatIconModule, MatPaginatorModule, MatProgressBarModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lista-contas.html',
  styleUrl: './lista-contas.scss',
})
export class ListaContas {
  private readonly api = inject(ContasApi);

  protected readonly rotulos = ROTULOS_PERFIL;
  protected readonly situacao = situacaoConta;
  protected readonly pagina = signal<Pagina<Conta> | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly tamanho = signal(20);

  constructor() {
    this.carregar(0);
  }

  protected mudarPagina(evento: PageEvent): void {
    this.tamanho.set(evento.pageSize);
    this.carregar(evento.pageIndex);
  }

  private carregar(numero: number): void {
    this.carregando.set(true);
    this.api.listar(numero, this.tamanho()).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.erro.set(null);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar as contas.'));
        this.carregando.set(false);
      },
    });
  }
}
