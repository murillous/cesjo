import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { TipoComunicacao } from './rotulos';

/** Campos do registro e da correção (ADR 0160). Data em `AAAA-MM-DD`. */
export interface DadosComunicacao {
  tipo: TipoComunicacao;
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
  data: string;
  contraparte: string;
  assunto: string;
}

export interface Comunicacao extends ResumoComunicacao {
  descricao: string | null;
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
  inicio?: string | null;
  fim?: string | null;
}

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
    return this.http.get<PaginaComunicacoes>('/api/comunicacoes', { params });
  }

  comunicacao(id: string): Observable<Comunicacao> {
    return this.http.get<Comunicacao>(`/api/comunicacoes/${encodeURIComponent(id)}`);
  }

  registrar(dados: DadosComunicacao): Observable<Comunicacao> {
    return this.http.post<Comunicacao>('/api/comunicacoes', dados);
  }

  corrigir(id: string, dados: DadosComunicacao): Observable<Comunicacao> {
    return this.http.put<Comunicacao>(`/api/comunicacoes/${encodeURIComponent(id)}`, dados);
  }
}
