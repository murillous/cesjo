import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { Sessao } from '../../acesso/sessao';
import { PaginadorPtBr } from '../../compartilhado/paginador-pt-br';
import { lerDataIso } from '../visitas/datas-visita';
import { AtividadesApi, ResumoAtividade } from './atividades-api';
import { ROTULOS_TIPO_ATIVIDADE } from './rotulos';

/**
 * Diário das atividades (RF010, ADR 0153): da mais recente para a mais antiga, com a data em
 * destaque à esquerda, porque a equipe procura "o que fizemos naquele dia". Sem a descrição.
 */
@Component({
  selector: 'app-lista-atividades',
  imports: [
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    RouterLink,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lista-atividades.html',
  styleUrl: './lista-atividades.scss',
})
export class ListaAtividades implements OnInit {
  private readonly api = inject(AtividadesApi);

  protected readonly podeRegistrar = inject(Sessao).temPermissao('ATIVIDADES_REGISTRAR');
  protected readonly tipos = ROTULOS_TIPO_ATIVIDADE;
  protected readonly data = lerDataIso;
  protected readonly tamanhos = [20, 50, 100];

  protected readonly atividades = signal<readonly ResumoAtividade[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(20);
  protected readonly carregando = signal(false);
  protected readonly erro = signal(false);

  ngOnInit(): void {
    this.carregar();
  }

  protected mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set(false);
    this.api.listar(this.pagina(), this.tamanho()).subscribe({
      next: (resposta) => {
        this.atividades.set(resposta.content);
        this.total.set(resposta.page.totalElements);
        this.carregando.set(false);
      },
      error: () => {
        this.atividades.set([]);
        this.total.set(0);
        this.erro.set(true);
        this.carregando.set(false);
      },
    });
  }

  /** "Ana, Bruno e mais 3", para a linha não crescer com grupos grandes. */
  protected nomes(atividade: ResumoAtividade): string {
    const primeiros = atividade.participantes.slice(0, 2).map((p) => p.nomeCompleto.split(' ')[0]);
    const resto = atividade.participantes.length - primeiros.length;
    return resto > 0 ? `${primeiros.join(', ')} e mais ${resto}` : primeiros.join(' e ');
  }
}
