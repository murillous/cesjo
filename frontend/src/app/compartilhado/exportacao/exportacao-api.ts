import { HttpClient, HttpErrorResponse, HttpParams, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, from, map, Observable, switchMap, throwError } from 'rxjs';

import {
  ArquivoBaixado,
  nomeDoContentDisposition,
  salvarNoNavegador,
} from '../arquivos/arquivos-api';

/** Formatos aceitos pelo parâmetro `formato` dos endpoints de exportação (ADR 0042). */
export type FormatoExportacao = 'pdf' | 'xlsx';

/** Filtros da tela repassados como parâmetros da URL. Valores vazios não são enviados. */
export type ParametrosExportacao = Readonly<
  Record<string, string | number | boolean | null | undefined>
>;

/**
 * Baixa as exportações em PDF e Excel dos módulos (ADR 0044). Cada módulo expõe um `GET` próprio
 * que recebe `formato` e os mesmos filtros da listagem; o backend checa a permissão e audita.
 */
@Injectable({ providedIn: 'root' })
export class ExportacaoApi {
  private readonly http = inject(HttpClient);

  baixar(
    url: string,
    formato: FormatoExportacao,
    parametros: ParametrosExportacao = {},
  ): Observable<ArquivoBaixado> {
    let params = new HttpParams().set('formato', formato);
    for (const [nome, valor] of Object.entries(parametros)) {
      if (valor !== null && valor !== undefined && valor !== '') {
        params = params.set(nome, String(valor));
      }
    }
    return this.http.get(url, { params, observe: 'response', responseType: 'blob' }).pipe(
      map((resposta: HttpResponse<Blob>) => ({
        nome:
          nomeDoContentDisposition(resposta.headers.get('Content-Disposition')) ??
          `exportacao.${formato}`,
        conteudo: resposta.body ?? new Blob(),
      })),
      catchError((erro: unknown) => lerProblemaDoBlob(erro)),
    );
  }

  /** Baixa e entrega ao navegador para salvar com o nome sugerido pelo backend. */
  baixarESalvar(
    url: string,
    formato: FormatoExportacao,
    parametros: ParametrosExportacao = {},
  ): Observable<ArquivoBaixado> {
    return this.baixar(url, formato, parametros).pipe(
      map((arquivo) => {
        salvarNoNavegador(arquivo);
        return arquivo;
      }),
    );
  }
}

/**
 * Com `responseType: 'blob'`, o `ProblemDetail` do erro chega como Blob. Converte para objeto, para
 * `mensagemDeErro` mostrar o `detail` do backend.
 */
function lerProblemaDoBlob(erro: unknown): Observable<never> {
  if (!(erro instanceof HttpErrorResponse) || !(erro.error instanceof Blob)) {
    return throwError(() => erro);
  }
  return from(erro.error.text()).pipe(
    switchMap((texto) => {
      let corpo: unknown = null;
      try {
        corpo = JSON.parse(texto);
      } catch {
        // Corpo sem JSON: fica só o status.
      }
      return throwError(
        () =>
          new HttpErrorResponse({
            error: corpo,
            headers: erro.headers,
            status: erro.status,
            statusText: erro.statusText,
            url: erro.url ?? undefined,
          }),
      );
    }),
  );
}
