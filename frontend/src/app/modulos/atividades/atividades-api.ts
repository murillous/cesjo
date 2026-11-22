import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Profissional } from '../visitas/visitas-api';
import { TipoAtividade } from './rotulos';

/** Usuário atendido que participou da atividade. */
export interface Participante {
  pessoaId: string;
  nomeCompleto: string;
}

/** Campos do registro e da correção (ADR 0150). Datas em `AAAA-MM-DD`. */
export interface DadosAtividade {
  tipo: TipoAtividade;
  data: string;
  titulo: string;
  descricao: string;
  /** `pessoaId` dos usuários atendidos. */
  participantes: string[];
  /** `id` dos colaboradores. */
  profissionais: string[];
}

/** Atividade na listagem, sem a descrição. */
export interface ResumoAtividade {
  id: string;
  tipo: TipoAtividade;
  data: string;
  titulo: string;
  participantes: Participante[];
  profissionais: Profissional[];
}

export interface Atividade extends ResumoAtividade {
  descricao: string;
  criadoEm: string;
  atualizadoEm: string;
}

export interface PaginaAtividades {
  content: readonly ResumoAtividade[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

/** O que pode ser escolhido no registro: usuários ativos e profissionais ativos. */
export interface OpcoesAtividade {
  usuarios: Participante[];
  profissionais: Profissional[];
  sugeridoId: string | null;
}

@Injectable({ providedIn: 'root' })
export class AtividadesApi {
  private readonly http = inject(HttpClient);

  listar(pagina: number, tamanho: number): Observable<PaginaAtividades> {
    const params = new HttpParams().set('page', pagina).set('size', tamanho);
    return this.http.get<PaginaAtividades>('/api/atividades', { params });
  }

  atividade(id: string): Observable<Atividade> {
    return this.http.get<Atividade>(`/api/atividades/${encodeURIComponent(id)}`);
  }

  registrar(dados: DadosAtividade): Observable<Atividade> {
    return this.http.post<Atividade>('/api/atividades', dados);
  }

  corrigir(id: string, dados: DadosAtividade): Observable<Atividade> {
    return this.http.put<Atividade>(`/api/atividades/${encodeURIComponent(id)}`, dados);
  }

  opcoes(): Observable<OpcoesAtividade> {
    return this.http.get<OpcoesAtividade>('/api/atividades/opcoes');
  }
}
