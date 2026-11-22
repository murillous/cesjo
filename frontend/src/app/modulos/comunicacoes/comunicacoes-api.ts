import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import {
  ArquivoBaixado,
  nomeDoContentDisposition,
} from '../../compartilhado/arquivos/arquivos-api';
import { DirecaoComunicacao, TipoComunicacao } from './rotulos';

/** Campos do registro e da correção (ADR 0160 e ADR 0164). Data em `AAAA-MM-DD`. */
export interface DadosComunicacao {
  tipo: TipoComunicacao;
  /** Só nos ofícios, declarações e encaminhamentos; `null` nos outros tipos. */
  direcao: DirecaoComunicacao | null;
  data: string;
  /** Pessoa ou instituição do outro lado. */
  contraparte: string;
  assunto: string;
  /** Opcional; o backend guarda cifrada. */
  descricao: string | null;
}

/** Contato ou comunicação na listagem, sem a descrição. */
export interface ResumoComunicacao {
  id: string;
  tipo: TipoComunicacao;
  direcao: DirecaoComunicacao | null;
  data: string;
  contraparte: string;
  assunto: string;
  temAnexo: boolean;
}

/** Anexo da ficha, sem o conteúdo. */
export interface AnexoComunicacao {
  nomeOriginal: string;
  tipo: 'PDF' | 'JPEG' | 'PNG';
  tamanho: number;
}

export interface Comunicacao extends Omit<ResumoComunicacao, 'temAnexo'> {
  descricao: string | null;
  anexo: AnexoComunicacao | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface PaginaComunicacoes {
  content: readonly ResumoComunicacao[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

/** Filtros da listagem; os vazios não vão para a API. Datas em `AAAA-MM-DD`. */
export interface FiltroComunicacoes {
  tipo?: TipoComunicacao | null;
  direcao?: DirecaoComunicacao | null;
  /** Trecho do nome da pessoa ou instituição. */
  contraparte?: string | null;
  inicio?: string | null;
  fim?: string | null;
}

const URL = '/api/comunicacoes';

@Injectable({ providedIn: 'root' })
export class ComunicacoesApi {
  private readonly http = inject(HttpClient);

  listar(
    filtro: FiltroComunicacoes,
    pagina: number,
    tamanho: number,
  ): Observable<PaginaComunicacoes> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho);
    for (const [chave, valor] of Object.entries(filtro)) {
      if (valor) {
        params = params.set(chave, valor);
      }
    }
    return this.http.get<PaginaComunicacoes>(URL, { params });
  }

  comunicacao(id: string): Observable<Comunicacao> {
    return this.http.get<Comunicacao>(`${URL}/${encodeURIComponent(id)}`);
  }

  registrar(dados: DadosComunicacao): Observable<Comunicacao> {
    return this.http.post<Comunicacao>(URL, dados);
  }

  corrigir(id: string, dados: DadosComunicacao): Observable<Comunicacao> {
    return this.http.put<Comunicacao>(`${URL}/${encodeURIComponent(id)}`, dados);
  }

  /** Anexa o arquivo, no lugar do anterior. O backend aceita PDF, JPG e PNG até 10 MB. */
  anexar(id: string, arquivo: File): Observable<Comunicacao> {
    const corpo = new FormData();
    corpo.append('arquivo', arquivo, arquivo.name);
    return this.http.post<Comunicacao>(`${URL}/${encodeURIComponent(id)}/anexo`, corpo);
  }

  removerAnexo(id: string): Observable<Comunicacao> {
    return this.http.delete<Comunicacao>(`${URL}/${encodeURIComponent(id)}/anexo`);
  }

  /** Baixa o anexo; o backend audita a leitura. */
  baixarAnexo(id: string): Observable<ArquivoBaixado> {
    return this.http
      .get(`${URL}/${encodeURIComponent(id)}/anexo`, { observe: 'response', responseType: 'blob' })
      .pipe(
        map((resposta: HttpResponse<Blob>) => ({
          nome: nomeDoContentDisposition(resposta.headers.get('Content-Disposition')) ?? 'anexo',
          conteudo: resposta.body ?? new Blob(),
        })),
      );
  }
}
