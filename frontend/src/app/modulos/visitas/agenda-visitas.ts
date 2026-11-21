import { DatePipe, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { dataIso, diaDe, inicioDaSemana, lerDataIso, somarDias } from './datas-visita';
import { Visita, VisitasApi } from './visitas-api';
import { VisitasDoDia } from './visitas-do-dia';

interface Dia {
  data: Date;
  iso: string;
  hoje: boolean;
  visitas: Visita[];
}

/**
 * Agenda das visitas domiciliares por semana, de segunda a domingo (issue #34). A semana vem do
 * parâmetro `?semana=AAAA-MM-DD` (qualquer dia dela); sem ele, a semana atual.
 */
@Component({
  selector: 'app-agenda-visitas',
  imports: [
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    RouterLink,
    TitleCasePipe,
    VisitasDoDia,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './agenda-visitas.html',
  styleUrl: './agenda-visitas.scss',
})
export class AgendaVisitas {
  /** Parâmetro `?semana=` da rota. */
  readonly semana = input<string>();

  private readonly api = inject(VisitasApi);
  private readonly router = inject(Router);

  protected readonly podeAgendar = inject(Sessao).temPermissao('VISITAS_AGENDAR');
  protected readonly inicio = computed(() =>
    inicioDaSemana(lerDataIso(this.semana()) ?? new Date()),
  );
  protected readonly fim = computed(() => somarDias(this.inicio(), 6));
  protected readonly ehSemanaAtual = computed(
    () => dataIso(this.inicio()) === dataIso(inicioDaSemana(new Date())),
  );
  protected readonly visitas = signal<Visita[] | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly dias = computed<Dia[]>(() => {
    const visitas = this.visitas() ?? [];
    const hoje = dataIso(new Date());
    return Array.from({ length: 7 }, (_, i) => {
      const data = somarDias(this.inicio(), i);
      const iso = dataIso(data);
      return {
        data,
        iso,
        hoje: iso === hoje,
        visitas: visitas.filter((v) => diaDe(v.dataHora) === iso),
      };
    });
  });

  constructor() {
    toObservable(this.inicio)
      .pipe(
        tap(() => this.carregando.set(true)),
        switchMap((inicio) =>
          this.api.agenda(dataIso(inicio), dataIso(somarDias(inicio, 6))).pipe(
            map((visitas) => ({ visitas, erro: null as string | null })),
            catchError((erro: unknown) =>
              of({
                visitas: [],
                erro: mensagemDeErro(erro, 'Não foi possível carregar a agenda.'),
              }),
            ),
          ),
        ),
      )
      .subscribe(({ visitas, erro }) => {
        this.visitas.set(visitas);
        this.erro.set(erro);
        this.carregando.set(false);
      });
  }

  protected irPara(semanas: number | null): void {
    const semana = semanas === null ? null : dataIso(somarDias(this.inicio(), semanas * 7));
    void this.router.navigate([], { queryParams: { semana }, replaceUrl: true });
  }
}
