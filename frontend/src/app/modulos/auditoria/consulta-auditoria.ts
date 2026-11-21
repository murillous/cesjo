import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';

import {
  AcaoAuditoria,
  AuditoriaApi,
  FiltrosAuditoria,
  RegistroAuditoria,
  ROTULOS_ACAO,
} from './auditoria-api';
import { PaginadorPtBr } from '../../compartilhado/paginador-pt-br';

/** Ações que pedem atenção de quem revisa o log: dado sensível, saída de dados e tentativa negada. */
const ACOES_EM_DESTAQUE: ReadonlySet<AcaoAuditoria> = new Set([
  'LER_SENSIVEL',
  'EXPORTAR',
  'LOGIN_FALHO',
  'EXCLUIR',
]);

/** Consulta do log de auditoria (RNF 3.3), só para quem tem `AUDITORIA_LER`. */
@Component({
  selector: 'app-consulta-auditoria',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './consulta-auditoria.html',
  styleUrl: './consulta-auditoria.scss',
})
export class ConsultaAuditoria implements OnInit {
  private readonly api = inject(AuditoriaApi);

  protected readonly rotulosAcao = ROTULOS_ACAO;
  protected readonly acoes = Object.keys(ROTULOS_ACAO) as AcaoAuditoria[];
  protected readonly colunas = ['ocorridoEm', 'acao', 'entidade', 'login', 'campos', 'ip'];
  protected readonly tamanhos = [20, 50, 100];

  protected readonly filtros = new FormGroup({
    inicio: new FormControl<Date | null>(null),
    fim: new FormControl<Date | null>(null),
    login: new FormControl(''),
    entidade: new FormControl<string | null>(null),
    acao: new FormControl<AcaoAuditoria | null>(null),
  });

  protected readonly entidades = signal<readonly string[]>([]);
  protected readonly registros = signal<readonly RegistroAuditoria[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(20);
  protected readonly carregando = signal(false);
  protected readonly erro = signal(false);

  /** Filtros da última busca; a paginação usa estes, não o que está digitado e não foi aplicado. */
  private aplicados: FiltrosAuditoria = {};

  ngOnInit(): void {
    this.api.entidades().subscribe({ next: (lista) => this.entidades.set(lista) });
    this.carregar();
  }

  protected filtrar(): void {
    this.aplicados = { ...this.filtros.getRawValue() };
    this.pagina.set(0);
    this.carregar();
  }

  protected limpar(): void {
    this.filtros.reset({ inicio: null, fim: null, login: '', entidade: null, acao: null });
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
        this.registros.set(resposta.content);
        this.total.set(resposta.page.totalElements);
        this.carregando.set(false);
      },
      error: () => {
        this.registros.set([]);
        this.total.set(0);
        this.erro.set(true);
        this.carregando.set(false);
      },
    });
  }

  protected rotulo(acao: AcaoAuditoria): string {
    return ROTULOS_ACAO[acao];
  }

  protected emDestaque(acao: AcaoAuditoria): boolean {
    return ACOES_EM_DESTAQUE.has(acao);
  }
}
