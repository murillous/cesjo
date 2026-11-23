import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subscription } from 'rxjs';

import { Sessao } from '../../acesso/sessao';
import { BotaoExportar } from '../../compartilhado/exportacao/botao-exportar';
import { ParametrosExportacao } from '../../compartilhado/exportacao/exportacao-api';
import { PaginadorPtBr } from '../../compartilhado/paginador-pt-br';
import { FiltrosFila, ItemFila, ListaEsperaApi } from './lista-espera-api';
import { PainelVagas } from './painel-vagas';
import {
  ORIGENS_ENCAMINHAMENTO,
  OrigemEncaminhamento,
  rotuloDe,
  SITUACOES,
  SituacaoInscricao,
  TIPOS_DEFICIENCIA,
  TipoDeficiencia,
} from './rotulos';

/** Espera depois da última tecla antes de buscar pelo nome. */
const ESPERA_BUSCA_MS = 300;

/**
 * Fila da lista de espera (`/lista-espera`, RF002): candidatos na ordem da fila, com a posição, busca
 * por nome e filtros. No celular cada candidato é um cartão; em telas largas, uma tabela. Quem só tem
 * `LISTA_ESPERA_FILA_LER` (perfil Consulta) vê os dados básicos e filtra só por nome e situação
 * (ADR 0058).
 */
@Component({
  selector: 'app-fila-lista-espera',
  imports: [
    BotaoExportar,
    DatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    PainelVagas,
    ReactiveFormsModule,
    RouterLink,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './fila-lista-espera.html',
  styleUrl: './fila-lista-espera.scss',
})
export class FilaListaEspera implements OnInit {
  private readonly api = inject(ListaEsperaApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sessao = inject(Sessao);

  protected readonly podeCadastrar = this.sessao.temPermissao('LISTA_ESPERA_CADASTRAR');
  /** Só o Coordenador importa planilhas (ADR 0216). */
  protected readonly podeImportar = this.sessao.temPermissao('PLANILHAS_IMPORTAR');
  protected readonly podeVerVagas = this.sessao.temPermissao('CAPACIDADE_LER');
  /** O Consulta não exporta (ADR 0130). */
  protected readonly podeExportar = this.sessao.temPermissao('LISTA_ESPERA_EXPORTAR');
  /** Filtros da última busca, para a exportação sair igual à tela (ADR 0130). */
  protected readonly parametrosExportacao = signal<ParametrosExportacao>({});
  /** Ficha completa: vê tipo, origem e cadastro incompleto, e filtra por eles. */
  protected readonly completo = this.sessao.temPermissao('LISTA_ESPERA_LER');

  protected readonly tipos = TIPOS_DEFICIENCIA;
  protected readonly origens = ORIGENS_ENCAMINHAMENTO;
  protected readonly situacoes = SITUACOES;
  protected readonly tamanhos = [20, 50, 100];

  protected readonly filtros = new FormGroup({
    nome: new FormControl('', { nonNullable: true }),
    tipoDeficiencia: new FormControl<TipoDeficiencia | null>(null),
    origem: new FormControl<OrigemEncaminhamento | null>(null),
    situacao: new FormControl<SituacaoInscricao | null>(null),
  });

  protected readonly itens = signal<readonly ItemFila[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(20);
  protected readonly carregando = signal(false);
  protected readonly erro = signal(false);
  protected readonly filtrosAbertos = signal(false);

  private pedido?: Subscription;

  ngOnInit(): void {
    this.filtros.controls.nome.valueChanges
      .pipe(
        debounceTime(ESPERA_BUSCA_MS),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.buscar());
    this.carregar();
  }

  /** Aplica busca e filtros a partir da primeira página. */
  protected buscar(): void {
    this.pagina.set(0);
    this.carregar();
  }

  protected limpar(): void {
    this.filtros.reset({ nome: '', tipoDeficiencia: null, origem: null, situacao: null });
    this.buscar();
  }

  protected temFiltro(): boolean {
    const { nome, tipoDeficiencia, origem, situacao } = this.filtros.getRawValue();
    return !!nome.trim() || !!tipoDeficiencia || !!origem || !!situacao;
  }

  protected mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  protected carregar(): void {
    this.pedido?.unsubscribe();
    this.carregando.set(true);
    this.erro.set(false);
    const filtros = this.filtrosAplicados();
    this.parametrosExportacao.set({ ...filtros, nome: filtros.nome?.trim() || null });
    this.pedido = this.api.listar(filtros, this.pagina(), this.tamanho()).subscribe({
      next: (resposta) => {
        this.itens.set(resposta.content);
        this.total.set(resposta.page.totalElements);
        this.carregando.set(false);
      },
      error: () => {
        this.itens.set([]);
        this.total.set(0);
        this.erro.set(true);
        this.carregando.set(false);
      },
    });
  }

  protected tipo(valor: TipoDeficiencia | undefined): string {
    return rotuloDe(TIPOS_DEFICIENCIA, valor);
  }

  protected origem(valor: OrigemEncaminhamento | undefined): string {
    return rotuloDe(ORIGENS_ENCAMINHAMENTO, valor);
  }

  protected situacao(valor: SituacaoInscricao): string {
    return rotuloDe(SITUACOES, valor);
  }

  private filtrosAplicados(): FiltrosFila {
    const { nome, tipoDeficiencia, origem, situacao } = this.filtros.getRawValue();
    return this.completo ? { nome, tipoDeficiencia, origem, situacao } : { nome, situacao };
  }
}
