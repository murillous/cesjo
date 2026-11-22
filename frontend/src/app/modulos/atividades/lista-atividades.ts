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
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Sessao } from '../../acesso/sessao';
import { PaginadorPtBr } from '../../compartilhado/paginador-pt-br';
import { ROTULOS_FUNCAO } from '../colaboradores/rotulos';
import { dataIso, lerDataIso } from '../visitas/datas-visita';
import { AtividadesApi, FiltrosAtividades, OpcoesFiltro, ResumoAtividade } from './atividades-api';
import { descricaoTipo, ROTULOS_TIPO_ATIVIDADE, TipoAtividade, TIPOS_ATIVIDADE } from './rotulos';

/**
 * Diário das atividades (RF010, ADR 0153): da mais recente para a mais antiga, com a data em
 * destaque à esquerda, porque a equipe procura "o que fizemos naquele dia". Sem a descrição.
 * Filtros por tipo, período, profissional e usuário (ADR 0155); `?pessoaId=` já abre filtrado, como
 * no link da ficha do usuário atendido.
 */
@Component({
  selector: 'app-lista-atividades',
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
  templateUrl: './lista-atividades.html',
  styleUrl: './lista-atividades.scss',
})
export class ListaAtividades implements OnInit {
  private readonly api = inject(AtividadesApi);

  protected readonly podeRegistrar = inject(Sessao).temPermissao('ATIVIDADES_REGISTRAR');
  protected readonly tipos = ROTULOS_TIPO_ATIVIDADE;
  protected readonly data = lerDataIso;
  protected readonly tamanhos = [20, 50, 100];
  protected readonly listaTipos = TIPOS_ATIVIDADE;
  protected readonly funcoes = ROTULOS_FUNCAO;
  protected readonly opcoes = signal<OpcoesFiltro>({ profissionais: [], usuarios: [] });

  protected readonly filtros = new FormGroup({
    tipo: new FormControl<TipoAtividade | null>(null),
    de: new FormControl<Date | null>(null),
    ate: new FormControl<Date | null>(null),
    profissionalId: new FormControl<string | null>(null),
    pessoaId: new FormControl<string | null>(
      inject(ActivatedRoute).snapshot.queryParamMap.get('pessoaId'),
    ),
  });

  /** Filtros da última busca; a paginação usa estes, não o que está escolhido e não foi aplicado. */
  private aplicados: FiltrosAtividades = this.lerFiltros();

  protected readonly atividades = signal<readonly ResumoAtividade[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(20);
  protected readonly carregando = signal(false);
  protected readonly erro = signal(false);

  ngOnInit(): void {
    this.carregar();
    this.api.filtros().subscribe({ next: (opcoes) => this.opcoes.set(opcoes) });
  }

  protected filtrar(): void {
    const { de, ate } = this.filtros.getRawValue();
    if (de && ate && ate < de) {
      this.filtros.controls.ate.setErrors({ antesDoInicio: true });
      this.filtros.controls.ate.markAsTouched();
      return;
    }
    this.aplicados = this.lerFiltros();
    this.pagina.set(0);
    this.carregar();
  }

  protected limpar(): void {
    this.filtros.reset({ tipo: null, de: null, ate: null, profissionalId: null, pessoaId: null });
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

  private lerFiltros(): FiltrosAtividades {
    const valor = this.filtros.getRawValue();
    return {
      tipo: valor.tipo,
      de: valor.de ? dataIso(valor.de) : null,
      ate: valor.ate ? dataIso(valor.ate) : null,
      profissionalId: valor.profissionalId,
      pessoaId: valor.pessoaId,
    };
  }

  /** Atendimento ou estudo de caso: abrir fica no log. */
  protected individual(atividade: ResumoAtividade): boolean {
    return !!descricaoTipo(atividade.tipo)?.individual;
  }

  /** "Ana, Bruno e mais 3", para a linha não crescer com grupos grandes. */
  protected nomes(atividade: ResumoAtividade): string {
    const primeiros = atividade.participantes.slice(0, 2).map((p) => p.nomeCompleto.split(' ')[0]);
    const resto = atividade.participantes.length - primeiros.length;
    return resto > 0 ? `${primeiros.join(', ')} e mais ${resto}` : primeiros.join(' e ');
  }
}
