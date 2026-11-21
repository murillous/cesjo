import { HttpClient, HttpEventType, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { filter, map, Observable } from 'rxjs';

import { TipoArquivoAceito } from './regras-arquivo';

/** Metadados devolvidos por `POST /api/arquivos`. */
export interface ArquivoEnviado {
  id: string;
  nome: string;
  tipoMime: TipoArquivoAceito;
  tamanho: number;
  sha256: string;
  enviadoEm: string;
}

/** Andamento de um envio: progresso de 0 a 100 e, no fim, os metadados do arquivo guardado. */
export type EventoEnvio =
  { tipo: 'progresso'; percentual: number } | { tipo: 'concluido'; arquivo: ArquivoEnviado };

/** Arquivo baixado, com o nome informado pelo backend no `Content-Disposition`. */
export interface ArquivoBaixado {
  nome: string;
  conteudo: Blob;
}

/**
 * Envio e download de arquivos pela API do módulo Arquivos (ADR 0028). O backend checa as permissões
 * `DOCUMENTOS_ENVIAR` e `DOCUMENTOS_LER` e audita o download.
 */
@Injectable({ providedIn: 'root' })
export class ArquivosApi {
  static readonly URL = '/api/arquivos';

  private readonly http = inject(HttpClient);

  enviar(arquivo: File): Observable<EventoEnvio> {
    const corpo = new FormData();
    corpo.append('arquivo', arquivo, arquivo.name);
    return this.http
      .post<ArquivoEnviado>(ArquivosApi.URL, corpo, { observe: 'events', reportProgress: true })
      .pipe(
        filter(
          (evento) =>
            evento.type === HttpEventType.UploadProgress || evento.type === HttpEventType.Response,
        ),
        map((evento): EventoEnvio => {
          if (evento.type === HttpEventType.Response) {
            return { tipo: 'concluido', arquivo: evento.body as ArquivoEnviado };
          }
          const total = evento.total ?? arquivo.size;
          const percentual =
            total > 0 ? Math.min(100, Math.round((evento.loaded / total) * 100)) : 0;
          return { tipo: 'progresso', percentual };
        }),
      );
  }

  baixar(id: string): Observable<ArquivoBaixado> {
    return this.http
      .get(`${ArquivosApi.URL}/${encodeURIComponent(id)}`, {
        observe: 'response',
        responseType: 'blob',
      })
      .pipe(
        map((resposta: HttpResponse<Blob>) => ({
          nome: nomeDoContentDisposition(resposta.headers.get('Content-Disposition')) ?? 'arquivo',
          conteudo: resposta.body ?? new Blob(),
        })),
      );
  }

  /** Baixa o arquivo e entrega ao navegador para salvar com o nome original. */
  baixarESalvar(id: string): Observable<ArquivoBaixado> {
    return this.baixar(id).pipe(
      map((arquivo) => {
        salvarNoNavegador(arquivo);
        return arquivo;
      }),
    );
  }
}

/** Lê o nome do `Content-Disposition`, preferindo `filename*` (UTF-8) a `filename`. */
export function nomeDoContentDisposition(cabecalho: string | null): string | null {
  if (!cabecalho) {
    return null;
  }
  const estendido = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(cabecalho);
  if (estendido) {
    try {
      return decodeURIComponent(estendido[1].trim());
    } catch {
      // Valor malformado: tenta o filename simples.
    }
  }
  const simples =
    /filename\s*=\s*"([^"]*)"/i.exec(cabecalho) ?? /filename\s*=\s*([^;]+)/i.exec(cabecalho);
  return simples ? simples[1].trim() : null;
}

/** Entrega o arquivo baixado ao navegador para salvar com o nome informado. */
export function salvarNoNavegador({ nome, conteudo }: ArquivoBaixado): void {
  const url = URL.createObjectURL(conteudo);
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  link.click();
  URL.revokeObjectURL(url);
}
