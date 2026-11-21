import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';

import { Sessao } from '../../acesso/sessao';
import { PaginadorPtBr } from '../auditoria/paginador-pt-br';
import { TIPOS_DEFICIENCIA, TipoDeficiencia } from '../lista-espera/rotulos';
import { Atendido, AtendidosApi, FiltrosAtendidos } from './atendidos-api';
import {
  FAIXAS_ETARIAS,
  FaixaEtaria,
  ROTULOS_SITUACAO_INSERCAO,
  rotuloTurno,
  SituacaoInsercao,
  Turno,
  TURNOS,
} from './rotulos';

/**
 * Listagem dos usuários atendidos (RF007, ADR 0083), em ordem de nome. Quem só tem
 * `ATENDIDOS_LER_BASICO` vê nome, situação, turno e data de inserção, e não filtra por tipo de
 * deficiência nem faixa etária (ADR 0082).
 */
@Component({
  selector: 'app-pagina-atendidos',
  imports: [
    DatePipe,
    MatButtonModule,
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
  templateUrl: './pagina-atendidos.html',
  styleUrl: './pagina-atendidos.scss',
})
export class PaginaAtendidos implements OnInit {
  private readonly api = inject(AtendidosApi);

  protected readonly completo = inject(Sessao).temPermissao('ATENDIDOS_LER');
  protected readonly turnos = TURNOS;
  protected readonly tipos = TIPOS_DEFICIENCIA;
  protected readonly faixas = FAIXAS_ETARIAS;
  protected readonly situacoes = ROTULOS_SITUACAO_INSERCAO;
  protected readonly rotuloTurno = rotuloTurno;
  protected readonly tamanhos = [20, 50, 100];

  protected readonly filtros = new FormGroup({
    nome: new FormControl(''),
    turno: new FormControl<Turno | null>(null),
    situacao: new FormControl<SituacaoInsercao | null>('ATIVA'),
    tipoDeficiencia: new FormControl<TipoDeficiencia | null>(null),
    faixaEtaria: new FormControl<FaixaEtaria | null>(null),
  });

  protected readonly atendidos = signal<readonly Atendido[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(20);
  protected readonly carregando = signal(false);
  protected readonly erro = signal(false);

  /** Filtros da última busca; a paginação usa estes, não o que está digitado e não foi aplicado. */
  private aplicados: FiltrosAtendidos = { situacao: 'ATIVA' };

  ngOnInit(): void {
    this.carregar();
  }

  protected filtrar(): void {
    const valores = this.filtros.getRawValue();
    this.aplicados = this.completo
      ? valores
      : { nome: valores.nome, turno: valores.turno, situacao: valores.situacao };
    this.pagina.set(0);
    this.carregar();
  }

  protected limpar(): void {
    this.filtros.reset({
      nome: '',
      turno: null,
      situacao: 'ATIVA',
      tipoDeficiencia: null,
      faixaEtaria: null,
    });
    this.filtrar();
  }

  protected mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set(false);
    this.api.listar(this.aplicados, this.pagina(), this.tamanho()).subscribe({
      next: (resposta) => {
        this.atendidos.set(resposta.content);
        this.total.set(resposta.page.totalElements);
        this.carregando.set(false);
      },
      error: () => {
        this.atendidos.set([]);
        this.total.set(0);
        this.erro.set(true);
        this.carregando.set(false);
      },
    });
  }

  protected rotuloTipo(tipo: TipoDeficiencia): string {
    return TIPOS_DEFICIENCIA.find((t) => t.valor === tipo)?.rotulo ?? tipo;
  }
}
