import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina } from '../contas/contas-api';
import { TipoParceiro } from './rotulos';

/** Parceiro como `GET /api/parceiros` devolve (ADR 0172). Datas em `AAAA-MM-DD`. */
export interface Parceiro {
  id: string;
  nome: string;
  tipo: TipoParceiro;
  instrumento: string | null;
  vigenciaInicio: string | null;
  vigenciaFim: string | null;
  contato: string | null;
  observacoes: string | null;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

/** Dados do cadastro e da edição; `ativo` só vale na edição. */
export interface DadosParceiro {
  nome: string;
  tipo: TipoParceiro;
  instrumento: string | null;
  vigenciaInicio: string | null;
  vigenciaFim: string | null;
  contato: string | null;
  observacoes: string | null;
  ativo?: boolean;
}

/** Ação de articulação com o parceiro (ADR 0172). */
export interface Articulacao {
  id: string;
  data: string;
  descricao: string;
  criadoEm: string;
}

@Injectable({ providedIn: 'root' })
export class ParceriasApi {
  private readonly http = inject(HttpClient);

  /** `ativo` nulo traz ativos e inativos. */
  listar(ativo: boolean | null, pagina: number, tamanho: number): Observable<Pagina<Parceiro>> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho).set('sort', 'nome');
    if (ativo !== null) {
      params = params.set('ativo', ativo);
    }
    return this.http.get<Pagina<Parceiro>>('/api/parceiros', { params });
  }

  obter(id: string): Observable<Parceiro> {
    return this.http.get<Parceiro>(`/api/parceiros/${encodeURIComponent(id)}`);
  }

  criar(dados: DadosParceiro): Observable<Parceiro> {
    return this.http.post<Parceiro>('/api/parceiros', dados);
  }

  editar(id: string, dados: DadosParceiro): Observable<Parceiro> {
    return this.http.put<Parceiro>(`/api/parceiros/${encodeURIComponent(id)}`, dados);
  }

  /** Da mais recente à mais antiga. */
  articulacoes(id: string): Observable<Articulacao[]> {
    return this.http.get<Articulacao[]>(`/api/parceiros/${encodeURIComponent(id)}/articulacoes`);
  }

  registrarArticulacao(id: string, data: string, descricao: string): Observable<Articulacao> {
    return this.http.post<Articulacao>(`/api/parceiros/${encodeURIComponent(id)}/articulacoes`, {
      data,
      descricao,
    });
  }
}
