import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina } from '../contas/contas-api';
import { DirecaoDoacao, TipoDoacao } from './rotulos';

/** Doação como `GET /api/doacoes` devolve (ADR 0178). */
export interface Doacao {
  id: string;
  direcao: DirecaoDoacao;
  /** `AAAA-MM-DD`. */
  data: string;
  contraparte: string;
  tipo: TipoDoacao;
  descricao: string;
  quantidade: string | null;
  /** Em reais, só na doação em dinheiro. */
  valor: number | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface DadosDoacao {
  direcao: DirecaoDoacao;
  data: string;
  contraparte: string;
  tipo: TipoDoacao;
  descricao: string;
  quantidade: string | null;
  valor: number | null;
}

/** Filtros da listagem; vazios não filtram. Datas em `AAAA-MM-DD`, inclusivas. */
export interface FiltroDoacoes {
  de: string | null;
  ate: string | null;
  direcao: DirecaoDoacao | null;
}

@Injectable({ providedIn: 'root' })
export class DoacoesApi {
  private readonly http = inject(HttpClient);

  /** Da mais recente à mais antiga. */
  listar(filtro: FiltroDoacoes, pagina: number, tamanho: number): Observable<Pagina<Doacao>> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho);
    for (const [chave, valor] of Object.entries(filtro)) {
      if (valor) {
        params = params.set(chave, valor as string);
      }
    }
    return this.http.get<Pagina<Doacao>>('/api/doacoes', { params });
  }

  obter(id: string): Observable<Doacao> {
    return this.http.get<Doacao>(`/api/doacoes/${encodeURIComponent(id)}`);
  }

  registrar(dados: DadosDoacao): Observable<Doacao> {
    return this.http.post<Doacao>('/api/doacoes', dados);
  }

  editar(id: string, dados: DadosDoacao): Observable<Doacao> {
    return this.http.put<Doacao>(`/api/doacoes/${encodeURIComponent(id)}`, dados);
  }
}
