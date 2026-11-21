import { DatePipe, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { ROTULOS_FUNCAO } from '../colaboradores/rotulos';
import { diaDe, horaDe, lerDataIso } from './datas-visita';
import { ROTULOS_STATUS } from './rotulos';
import { Visita, VisitasApi } from './visitas-api';

/** Ficha de uma visita domiciliar (`/visitas/:id`). */
@Component({
  selector: 'app-ficha-visita',
  imports: [DatePipe, MatButtonModule, MatIconModule, RouterLink, TitleCasePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ficha-visita.html',
  styleUrl: './ficha-visita.scss',
})
export class FichaVisita {
  /** Parâmetro `:id` da rota. */
  readonly id = input.required<string>();

  private readonly api = inject(VisitasApi);

  protected readonly funcoes = ROTULOS_FUNCAO;
  protected readonly rotulos = ROTULOS_STATUS;
  protected readonly visita = signal<Visita | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly data = computed(() => {
    const visita = this.visita();
    return visita ? lerDataIso(diaDe(visita.dataHora)) : null;
  });
  protected readonly hora = computed(() => {
    const visita = this.visita();
    return visita ? horaDe(visita.dataHora) : '';
  });
  /** Dia da visita, para voltar à semana certa da agenda. */
  protected readonly semana = computed(() => {
    const visita = this.visita();
    return visita ? diaDe(visita.dataHora) : null;
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        filter((id) => !!id),
        switchMap((id) => this.api.obter(id)),
      )
      .subscribe({
        next: (visita) => this.mostrar(visita),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar a visita.')),
      });
  }

  protected mostrar(visita: Visita): void {
    this.visita.set(visita);
    this.erro.set(null);
  }
}
