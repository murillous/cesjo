import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

/** Notificação como a conta logada a vê (`GET /api/notificacoes`, ADR 0032). */
export interface Notificacao {
  id: string;
  tipo: string;
  mensagem: string;
  /** Caminho de uma tela do sistema relacionada à notificação, ou nulo. */
  referencia: string | null;
  criadaEm: string;
  lidaEm: string | null;
  lida: boolean;
}

/** Página no formato de paginação do backend (`content` e `page`). */
export interface PaginaNotificacoes {
  content: Notificacao[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

const BASE = '/api/notificacoes';

/** Cliente HTTP da caixa de notificações da conta logada. */
@Injectable({ providedIn: 'root' })
export class NotificacoesApi {
  private readonly http = inject(HttpClient);

  listar(pagina: number, tamanho: number): Observable<PaginaNotificacoes> {
    return this.http.get<PaginaNotificacoes>(BASE, {
      params: { page: pagina, size: tamanho },
    });
  }

  contarNaoLidas(): Observable<number> {
    return this.http
      .get<{ quantidade: number }>(`${BASE}/nao-lidas/contagem`)
      .pipe(map((resposta) => resposta.quantidade));
  }

  marcarComoLida(id: string): Observable<void> {
    return this.http.post<void>(`${BASE}/${encodeURIComponent(id)}/leitura`, null);
  }

  marcarTodasComoLidas(): Observable<number> {
    return this.http
      .post<{ marcadas: number }>(`${BASE}/leitura`, null)
      .pipe(map((resposta) => resposta.marcadas));
  }
}
