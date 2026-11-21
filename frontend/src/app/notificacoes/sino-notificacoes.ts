import { CdkTrapFocus } from '@angular/cdk/a11y';
import { CdkConnectedOverlay, CdkOverlayOrigin, ConnectedPosition } from '@angular/cdk/overlay';
import { DatePipe, DOCUMENT } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  InjectionToken,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import {
  catchError,
  EMPTY,
  filter,
  fromEvent,
  interval,
  merge,
  of,
  startWith,
  switchMap,
} from 'rxjs';

import { Sessao } from '../acesso/sessao';
import { Notificacao, NotificacoesApi } from './notificacoes-api';

/**
 * Intervalo da consulta periódica do contador de não lidas, em milissegundos (ADR 0034).
 * A consulta pausa enquanto a aba está escondida e roda de novo assim que ela volta.
 */
export const INTERVALO_CONSULTA_NOTIFICACOES = new InjectionToken<number>(
  'INTERVALO_CONSULTA_NOTIFICACOES',
  { factory: () => 60_000 },
);

/** Quantas notificações o painel carrega por vez. */
export const TAMANHO_PAGINA_NOTIFICACOES = 20;

type EstadoLista = 'carregando' | 'pronta' | 'erro';

/**
 * Sino de notificações da barra superior: contador de não lidas e painel com a lista. Só aparece
 * para conta autenticada com `NOTIFICACOES_LER`.
 */
@Component({
  selector: 'app-sino-notificacoes',
  imports: [
    CdkConnectedOverlay,
    CdkOverlayOrigin,
    CdkTrapFocus,
    DatePipe,
    MatButtonModule,
    MatIconModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sino-notificacoes.html',
  styleUrl: './sino-notificacoes.scss',
})
export class SinoNotificacoes {
  private readonly sessao = inject(Sessao);
  private readonly api = inject(NotificacoesApi);
  private readonly router = inject(Router);
  private readonly documento = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);
  private readonly intervalo = inject(INTERVALO_CONSULTA_NOTIFICACOES);

  protected readonly visivel = computed(
    () => this.sessao.autenticada() && this.sessao.temPermissao('NOTIFICACOES_LER'),
  );

  protected readonly naoLidas = signal(0);
  protected readonly aberto = signal(false);
  protected readonly notificacoes = signal<readonly Notificacao[]>([]);
  protected readonly estado = signal<EstadoLista>('carregando');
  protected readonly temMais = signal(false);
  private pagina = 0;

  protected readonly contadorExibido = computed(() =>
    this.naoLidas() > 99 ? '99+' : String(this.naoLidas()),
  );

  protected readonly rotuloBotao = computed(() => {
    const quantidade = this.naoLidas();
    if (quantidade === 0) {
      return 'Notificações, nenhuma não lida';
    }
    return quantidade === 1 ? 'Notificações, 1 não lida' : `Notificações, ${quantidade} não lidas`;
  });

  /** Texto da região viva: o leitor de tela anuncia quando a contagem muda. */
  protected readonly anuncio = computed(() => {
    const quantidade = this.naoLidas();
    if (quantidade === 0) {
      return '';
    }
    return quantidade === 1 ? '1 notificação não lida' : `${quantidade} notificações não lidas`;
  });

  protected readonly posicoes: ConnectedPosition[] = [
    { originX: 'end', originY: 'bottom', overlayX: 'end', overlayY: 'top', offsetY: 4 },
  ];

  constructor() {
    const abaVisivel = fromEvent(this.documento, 'visibilitychange').pipe(
      filter(() => this.documento.visibilityState === 'visible'),
    );

    toObservable(this.visivel)
      .pipe(
        switchMap((ativo) =>
          ativo
            ? merge(interval(this.intervalo), abaVisivel).pipe(
                startWith(0),
                filter(() => this.documento.visibilityState !== 'hidden'),
              )
            : EMPTY,
        ),
        switchMap(() => this.api.contarNaoLidas().pipe(catchError(() => EMPTY))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((quantidade) => this.naoLidas.set(quantidade));

    toObservable(this.visivel)
      .pipe(
        filter((ativo) => !ativo),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.aberto.set(false);
        this.naoLidas.set(0);
        this.notificacoes.set([]);
      });
  }

  protected alternar(): void {
    if (this.aberto()) {
      this.fechar();
    } else {
      this.aberto.set(true);
      this.carregar(0);
    }
  }

  protected fechar(): void {
    this.aberto.set(false);
  }

  protected carregarMais(): void {
    this.carregar(this.pagina + 1);
  }

  protected tentarDeNovo(): void {
    this.carregar(0);
  }

  protected marcarComoLida(notificacao: Notificacao): void {
    if (notificacao.lida) {
      return;
    }
    this.api.marcarComoLida(notificacao.id).subscribe(() => {
      this.atualizarItem(notificacao.id);
      this.naoLidas.update((quantidade) => Math.max(0, quantidade - 1));
    });
  }

  protected marcarTodasComoLidas(): void {
    this.api.marcarTodasComoLidas().subscribe(() => {
      const agora = new Date().toISOString();
      this.notificacoes.update((lista) =>
        lista.map((item) => (item.lida ? item : { ...item, lida: true, lidaEm: agora })),
      );
      this.naoLidas.set(0);
    });
  }

  /** Abre a tela relacionada, marcando a notificação como lida. */
  protected abrir(notificacao: Notificacao): void {
    this.marcarComoLida(notificacao);
    this.fechar();
    if (notificacao.referencia) {
      void this.router.navigateByUrl(notificacao.referencia);
    }
  }

  private carregar(pagina: number): void {
    if (pagina === 0) {
      this.estado.set('carregando');
    }
    this.api
      .listar(pagina, TAMANHO_PAGINA_NOTIFICACOES)
      .pipe(catchError(() => of(null)))
      .subscribe((resposta) => {
        if (!resposta) {
          this.estado.set('erro');
          return;
        }
        this.pagina = resposta.page.number;
        this.notificacoes.update((lista) =>
          pagina === 0 ? resposta.content : [...lista, ...resposta.content],
        );
        this.temMais.set(resposta.page.number + 1 < resposta.page.totalPages);
        this.estado.set('pronta');
      });
    if (pagina === 0) {
      this.api
        .contarNaoLidas()
        .pipe(catchError(() => EMPTY))
        .subscribe((quantidade) => this.naoLidas.set(quantidade));
    }
  }

  private atualizarItem(id: string): void {
    const agora = new Date().toISOString();
    this.notificacoes.update((lista) =>
      lista.map((item) => (item.id === id ? { ...item, lida: true, lidaEm: agora } : item)),
    );
  }
}
