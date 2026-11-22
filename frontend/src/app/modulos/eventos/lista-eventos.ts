import { DatePipe } from '@angular/common';
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
import { EventoInstitucional, EventosApi, FiltroEventos } from './eventos-api';
import { ROTULOS_TIPO_EVENTO, rotuloDirecao, TIPOS_EVENTO, TipoEvento } from './rotulos';

/**
 * Reuniões e eventos do mais recente ao mais antigo, com filtro por tipo e período (RF012,
 * ADR 0177).
 */
@Component({
  selector: 'app-lista-eventos',
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
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lista-eventos.html',
  styleUrl: './lista-eventos.scss',
})
export class ListaEventos {
  private readonly api = inject(EventosApi);

  protected readonly podeRegistrar = inject(Sessao).temPermissao('EVENTOS_REGISTRAR');
  protected readonly tipos = TIPOS_EVENTO;
  protected readonly rotulosTipo = ROTULOS_TIPO_EVENTO;
  protected readonly rotuloDirecao = rotuloDirecao;
  protected readonly pagina = signal<Pagina<EventoInstitucional> | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly tamanho = signal(20);
  private filtroAplicado: FiltroEventos = { tipo: null, de: null, ate: null };

  protected readonly filtros = inject(NonNullableFormBuilder).group({
    tipo: ['' as TipoEvento | ''],
    de: new FormControl<Date | null>(null),
    ate: new FormControl<Date | null>(null),
  });

  constructor() {
    this.carregar(0);
  }

  protected filtrar(): void {
    const { tipo, de, ate } = this.filtros.getRawValue();
    this.filtroAplicado = {
      tipo: tipo || null,
      de: de ? dataIso(de) : null,
      ate: ate ? dataIso(ate) : null,
    };
    this.carregar(0);
  }

  protected limpar(): void {
    this.filtros.reset({ tipo: '', de: null, ate: null });
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
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar as reuniões e eventos.'));
        this.carregando.set(false);
      },
    });
  }
}
