import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina } from '../contas/contas-api';
import { FuncaoColaborador } from './rotulos';

/** Conta de acesso ligada ao colaborador (ADR 0046). */
export interface ContaLigada {
  id: string;
  login: string;
  nome: string;
  ativa: boolean;
}

/** Colaborador como `GET /api/colaboradores` devolve (ADR 0045). */
export interface Colaborador {
  id: string;
  nome: string;
  funcao: FuncaoColaborador;
  ativo: boolean;
  conta: ContaLigada | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface DadosColaborador {
  nome: string;
  funcao: FuncaoColaborador;
  contaId: string | null;
}

/** Filtro da listagem: `null` traz ativos e inativos. */
export type FiltroSituacao = boolean | null;

@Injectable({ providedIn: 'root' })
export class ColaboradoresApi {
  private readonly http = inject(HttpClient);

  listar(ativo: FiltroSituacao, pagina: number, tamanho: number): Observable<Pagina<Colaborador>> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho).set('sort', 'nome');
    if (ativo !== null) {
      params = params.set('ativo', ativo);
    }
    return this.http.get<Pagina<Colaborador>>('/api/colaboradores', { params });
  }

  obter(id: string): Observable<Colaborador> {
    return this.http.get<Colaborador>(`/api/colaboradores/${encodeURIComponent(id)}`);
  }

  criar(dados: DadosColaborador): Observable<Colaborador> {
    return this.http.post<Colaborador>('/api/colaboradores', dados);
  }

  editar(id: string, dados: DadosColaborador & { ativo: boolean }): Observable<Colaborador> {
    return this.http.put<Colaborador>(`/api/colaboradores/${encodeURIComponent(id)}`, dados);
  }
}
