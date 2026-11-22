import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { map, Observable } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { PaginadorPtBr } from '../../compartilhado/paginador-pt-br';
import { dataIso, hoje } from '../lista-espera/datas';
import { AtendidosApi, FiltrosViolacoes, LinhaViolacao } from './atendidos-api';
import {
  AMBITOS_VIOLACAO,
  AmbitoViolacao,
  rotuloViolacao,
  TIPOS_VIOLACAO,
  TipoViolacao,
} from './rotulos';

/** Descrição aberta de uma violação, ou o erro ao lê-la. */
interface Leitura {
  carregando: boolean;
  descricao?: string;
  erro?: string;
}

/**
 * Consulta de violações de direitos (RF009, ADR 0145), com filtro por categoria e período. Com
 * `insercaoId`, mostra as violações da pessoa na ficha do usuário atendido; sem ele, a lista geral
 * paginada, com o nome de cada pessoa. A descrição só é buscada quando alguém pede para ler, porque
 * cada leitura fica registrada no log de auditoria.
 */
@Component({
  selector: 'app-painel-violacoes',
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
  templateUrl: './painel-violacoes.html',
  styleUrl: './painel-violacoes.scss',
})
export class PainelViolacoes implements OnInit {
  /** Inserção da ficha. Sem ela, o painel mostra a lista geral. */
  readonly insercaoId = input<string | null>(null);

  private readonly api = inject(AtendidosApi);

  protected readonly tipos = TIPOS_VIOLACAO;
  protected readonly rotuloViolacao = rotuloViolacao;
  protected readonly hoje = hoje();
  protected readonly tamanhos = [20, 50, 100];

  protected readonly filtros = new FormGroup({
    tipo: new FormControl<TipoViolacao | null>(null),
    de: new FormControl<Date | null>(null),
    ate: new FormControl<Date | null>(null),
  });

  protected readonly violacoes = signal<readonly LinhaViolacao[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(20);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly periodoInvalido = signal(false);
  protected readonly leituras = signal<Readonly<Record<string, Leitura>>>({});

  /** Filtros da última busca; a paginação usa estes, não o que está digitado e não foi aplicado. */
  private aplicados: FiltrosViolacoes = {};

  ngOnInit(): void {
    this.carregar();
  }

  protected geral(): boolean {
    return !this.insercaoId();
  }

  protected filtrar(): void {
    const { tipo, de, ate } = this.filtros.getRawValue();
    if (de && ate && de > ate) {
      this.periodoInvalido.set(true);
      return;
    }
    this.periodoInvalido.set(false);
    this.aplicados = { tipo, de: de ? dataIso(de) : null, ate: ate ? dataIso(ate) : null };
    this.pagina.set(0);
    this.carregar();
  }

  protected limpar(): void {
    this.filtros.reset({ tipo: null, de: null, ate: null });
    this.filtrar();
  }

  protected mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    const id = this.insercaoId();
    const pedido: Observable<{ linhas: readonly LinhaViolacao[]; total: number }> = id
      ? this.api
          .violacoesDoAtendido(id, this.aplicados)
          .pipe(map((linhas) => ({ linhas, total: linhas.length })))
      : this.api
          .listarViolacoes(this.aplicados, this.pagina(), this.tamanho())
          .pipe(map((pagina) => ({ linhas: pagina.content, total: pagina.page.totalElements })));
    pedido.subscribe({
      next: ({ linhas, total }) => {
        this.violacoes.set(linhas);
        this.total.set(total);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.violacoes.set([]);
        this.total.set(0);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar as violações.'));
        this.carregando.set(false);
      },
    });
  }

  /**
   * Abre ou fecha a descrição. Fechar descarta o texto da tela; abrir de novo busca outra vez, e o
   * backend registra cada leitura.
   */
  protected alternar(violacao: LinhaViolacao): void {
    const atual = this.leituras()[violacao.id];
    if (atual && !atual.carregando) {
      this.leituras.update((leituras) => {
        const outras = { ...leituras };
        delete outras[violacao.id];
        return outras;
      });
      return;
    }
    if (atual) {
      return;
    }
    this.definirLeitura(violacao.id, { carregando: true });
    this.api.lerViolacao(violacao.id).subscribe({
      next: (completa) =>
        this.definirLeitura(violacao.id, { carregando: false, descricao: completa.descricao }),
      error: (erro: unknown) =>
        this.definirLeitura(violacao.id, {
          carregando: false,
          erro: mensagemDeErro(erro, 'Não foi possível ler a descrição.'),
        }),
    });
  }

  protected rotuloAmbito(ambito: AmbitoViolacao): string {
    return AMBITOS_VIOLACAO.find((a) => a.valor === ambito)?.rotulo ?? ambito;
  }

  private definirLeitura(id: string, leitura: Leitura): void {
    this.leituras.update((leituras) => ({ ...leituras, [id]: leitura }));
  }
}
