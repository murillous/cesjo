import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { Pagina } from '../contas/contas-api';
import { dataIso } from '../lista-espera/datas';
import { Doacao, DoacoesApi, FiltroDoacoes } from './doacoes-api';
import { DirecaoDoacao, ROTULOS_DIRECAO_DOACAO, ROTULOS_TIPO_DOACAO } from './rotulos';

/** Doações da mais recente à mais antiga, com filtro por período e direção (RF015, ADR 0180). */
@Component({
  selector: 'app-lista-doacoes',
  imports: [
    CurrencyPipe,
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
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lista-doacoes.html',
  styleUrl: './lista-doacoes.scss',
})
export class ListaDoacoes {
  private readonly api = inject(DoacoesApi);

  protected readonly podeRegistrar = inject(Sessao).temPermissao('DOACOES_REGISTRAR');
  protected readonly direcoes = ROTULOS_DIRECAO_DOACAO;
  protected readonly tipos = ROTULOS_TIPO_DOACAO;
  protected readonly pagina = signal<Pagina<Doacao> | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly tamanho = signal(20);
  private filtroAplicado: FiltroDoacoes = { de: null, ate: null, direcao: null };

  protected readonly filtros = inject(NonNullableFormBuilder).group({
    de: new FormControl<Date | null>(null),
    ate: new FormControl<Date | null>(null),
    direcao: ['' as DirecaoDoacao | ''],
  });

  constructor() {
    this.carregar(0);
  }

  protected filtrar(): void {
    const { de, ate, direcao } = this.filtros.getRawValue();
    this.filtroAplicado = {
      de: de ? dataIso(de) : null,
      ate: ate ? dataIso(ate) : null,
      direcao: direcao || null,
    };
    this.carregar(0);
  }

  protected limpar(): void {
    this.filtros.reset({ de: null, ate: null, direcao: '' });
    this.filtrar();
  }

  protected mudarPagina(evento: PageEvent): void {
    this.tamanho.set(evento.pageSize);
    this.carregar(evento.pageIndex);
  }

  private carregar(numero: number): void {
    this.carregando.set(true);
    this.api.listar(this.filtroAplicado, numero, this.tamanho()).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.erro.set(null);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar as doações.'));
        this.carregando.set(false);
      },
    });
  }
}
