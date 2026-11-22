import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { PaginadorPtBr } from '../../compartilhado/paginador-pt-br';
import { dataIso, lerDataIso } from '../visitas/datas-visita';
import { ComunicacoesApi, FiltroComunicacoes, ResumoComunicacao } from './comunicacoes-api';
import { TIPO_POR_VALOR, TipoComunicacao, TIPOS_COMUNICACAO } from './rotulos';

/**
 * Registro de contatos e comunicações (RF011, ADR 0163): do mais recente para o mais antigo, cada um
 * com o ícone do tipo, a pessoa ou instituição e o assunto. Filtra por tipo e período. Sem a descrição.
 */
@Component({
  selector: 'app-lista-comunicacoes',
  imports: [
    DatePipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lista-comunicacoes.html',
  styleUrl: './lista-comunicacoes.scss',
})
export class ListaComunicacoes implements OnInit {
  private readonly api = inject(ComunicacoesApi);

  protected readonly podeRegistrar = inject(Sessao).temPermissao('COMUNICACOES_REGISTRAR');
  protected readonly tipos = TIPOS_COMUNICACAO;
  protected readonly tipoPorValor = TIPO_POR_VALOR;
  protected readonly data = lerDataIso;
  protected readonly tamanhos = [20, 50, 100];

  protected readonly filtros = inject(NonNullableFormBuilder).group({
    tipo: ['' as TipoComunicacao | ''],
    inicio: [null as Date | null],
    fim: [null as Date | null],
  });

  protected readonly comunicacoes = signal<readonly ResumoComunicacao[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(20);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly filtrado = signal(false);

  ngOnInit(): void {
    this.carregar();
  }

  protected filtrar(): void {
    this.pagina.set(0);
    this.carregar();
  }

  protected limpar(): void {
    this.filtros.reset();
    this.filtrar();
  }

  protected mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  protected carregar(): void {
    const filtro = this.filtro();
    this.filtrado.set(Object.values(filtro).some((v) => !!v));
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listar(filtro, this.pagina(), this.tamanho()).subscribe({
      next: (resposta) => {
        this.comunicacoes.set(resposta.content);
        this.total.set(resposta.page.totalElements);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.comunicacoes.set([]);
        this.total.set(0);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar os contatos.'));
        this.carregando.set(false);
      },
    });
  }

  private filtro(): FiltroComunicacoes {
    const valor = this.filtros.getRawValue();
    return {
      tipo: valor.tipo || null,
      inicio: valor.inicio ? dataIso(valor.inicio) : null,
      fim: valor.fim ? dataIso(valor.fim) : null,
    };
  }
}
