import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import {
  ArquivoBaixado,
  nomeDoContentDisposition,
  salvarNoNavegador,
} from '../arquivos/arquivos-api';

/** Coluna do modelo de planilha, como o backend descreve (ADR 0215). */
export interface ColunaModelo {
  titulo: string;
  obrigatoria: boolean;
  formato: string;
  exemplo: string;
  valoresAceitos: string[];
}

/** Uma linha da pré-visualização: valores lidos, na ordem de `Previa.colunas`, e erros. */
export interface LinhaPrevia {
  linha: number;
  valores: string[];
  erros: string[];
}

export interface Previa {
  colunas: string[];
  linhas: LinhaPrevia[];
  total: number;
  validas: number;
  comErro: number;
}

export interface ResultadoImportacao {
  importadas: number;
  ignoradas: number;
}

/** Tamanho máximo da planilha, o mesmo do backend (`LeitorPlanilha.TAMANHO_MAXIMO`, ADR 0218). */
export const TAMANHO_MAXIMO_PLANILHA = 2 * 1024 * 1024;

/**
 * Importação de planilhas Excel (ADR 0219). Cada importação tem uma URL base no backend, como
 * `/api/importacao/lista-espera`, com `colunas`, `modelo`, `previa` e a confirmação no `POST` da base.
 * O arquivo vai nas duas etapas; o backend não guarda nada entre elas.
 */
@Injectable({ providedIn: 'root' })
export class ImportacaoApi {
  private readonly http = inject(HttpClient);

  colunas(base: string): Observable<ColunaModelo[]> {
    return this.http.get<ColunaModelo[]>(`${base}/colunas`);
  }

  /** Baixa o modelo vazio e entrega ao navegador. */
  baixarModelo(base: string): Observable<ArquivoBaixado> {
    return this.http.get(`${base}/modelo`, { observe: 'response', responseType: 'blob' }).pipe(
      map((resposta: HttpResponse<Blob>) => {
        const arquivo = {
          nome:
            nomeDoContentDisposition(resposta.headers.get('Content-Disposition')) ?? 'modelo.xlsx',
          conteudo: resposta.body ?? new Blob(),
        };
        salvarNoNavegador(arquivo);
        return arquivo;
      }),
    );
  }

  previa(base: string, arquivo: File): Observable<Previa> {
    return this.http.post<Previa>(`${base}/previa`, formulario(arquivo));
  }

  importar(base: string, arquivo: File): Observable<ResultadoImportacao> {
    return this.http.post<ResultadoImportacao>(base, formulario(arquivo));
  }
}

function formulario(arquivo: File): FormData {
  const dados = new FormData();
  dados.append('arquivo', arquivo, arquivo.name);
  return dados;
}
