import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Perfil } from '../../acesso/permissao';

/** Conta de acesso como `GET /api/contas` devolve (ADR 0013). */
export interface Conta {
  id: string;
  login: string;
  nome: string;
  perfil: Perfil;
  ativa: boolean;
  bloqueada: boolean;
  trocaSenhaObrigatoria: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

/** Página no formato `via_dto` do Spring Data. */
export interface Pagina<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface ContaCriada {
  conta: Conta;
  senhaTemporaria: string;
}

@Injectable({ providedIn: 'root' })
export class ContasApi {
  private readonly http = inject(HttpClient);

  listar(pagina: number, tamanho: number): Observable<Pagina<Conta>> {
    const params = new HttpParams().set('page', pagina).set('size', tamanho).set('sort', 'nome');
    return this.http.get<Pagina<Conta>>('/api/contas', { params });
  }

  obter(id: string): Observable<Conta> {
    return this.http.get<Conta>(`/api/contas/${encodeURIComponent(id)}`);
  }

  criar(dados: { login: string; nome: string; perfil: Perfil }): Observable<ContaCriada> {
    return this.http.post<ContaCriada>('/api/contas', dados);
  }

  editar(id: string, dados: { nome: string; perfil: Perfil }): Observable<Conta> {
    return this.http.put<Conta>(`/api/contas/${encodeURIComponent(id)}`, dados);
  }

  desativar(id: string): Observable<Conta> {
    return this.http.post<Conta>(`/api/contas/${encodeURIComponent(id)}/desativar`, null);
  }

  reativar(id: string): Observable<Conta> {
    return this.http.post<Conta>(`/api/contas/${encodeURIComponent(id)}/reativar`, null);
  }

  redefinirSenha(id: string): Observable<{ senhaTemporaria: string }> {
    return this.http.post<{ senhaTemporaria: string }>(
      `/api/contas/${encodeURIComponent(id)}/redefinir-senha`,
      null,
    );
  }
}
