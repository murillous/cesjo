import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Valores de `AcaoAuditoria` no backend. */
export type AcaoAuditoria =
  | 'CRIAR'
  | 'ALTERAR'
  | 'EXCLUIR'
  | 'LER_SENSIVEL'
  | 'EXPORTAR'
  | 'IMPORTAR'
  | 'LOGIN'
  | 'LOGIN_FALHO'
  | 'LOGOUT';

/** Rótulo de cada ação nas telas, na ordem em que aparecem no filtro. */
export const ROTULOS_ACAO: Readonly<Record<AcaoAuditoria, string>> = {
  CRIAR: 'Criação',
  ALTERAR: 'Alteração',
  EXCLUIR: 'Exclusão',
  LER_SENSIVEL: 'Leitura de dado sensível',
  EXPORTAR: 'Exportação',
  IMPORTAR: 'Importação de planilha',
  LOGIN: 'Login',
  LOGIN_FALHO: 'Falha de login',
  LOGOUT: 'Saída',
};

/** Um registro do log, como devolvido por `GET /api/auditoria`. */
export interface RegistroAuditoria {
  id: string;
  ocorridoEm: string;
  contaId: string | null;
  login: string | null;
  acao: AcaoAuditoria;
  entidade: string;
  entidadeId: string | null;
  camposAlterados: readonly string[];
  ip: string | null;
}

export interface PaginaAuditoria {
  content: readonly RegistroAuditoria[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

/**
 * Filtros da consulta. As datas são dias do calendário do navegador; a API recebe instantes, com o
 * início do dia inicial e o início do dia seguinte ao final (ADR 0024).
 */
export interface FiltrosAuditoria {
  inicio?: Date | null;
  fim?: Date | null;
  login?: string | null;
  entidade?: string | null;
  acao?: AcaoAuditoria | null;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaApi {
  private readonly http = inject(HttpClient);

  listar(filtros: FiltrosAuditoria, pagina: number, tamanho: number): Observable<PaginaAuditoria> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho);
    if (filtros.inicio) {
      params = params.set('de', inicioDoDia(filtros.inicio).toISOString());
    }
    if (filtros.fim) {
      const seguinte = inicioDoDia(filtros.fim);
      seguinte.setDate(seguinte.getDate() + 1);
      params = params.set('ate', seguinte.toISOString());
    }
    const login = filtros.login?.trim();
    if (login) {
      params = params.set('login', login);
    }
    if (filtros.entidade) {
      params = params.set('entidade', filtros.entidade);
    }
    if (filtros.acao) {
      params = params.set('acao', filtros.acao);
    }
    return this.http.get<PaginaAuditoria>('/api/auditoria', { params });
  }

  entidades(): Observable<readonly string[]> {
    return this.http.get<readonly string[]>('/api/auditoria/entidades');
  }
}

function inicioDoDia(data: Date): Date {
  return new Date(data.getFullYear(), data.getMonth(), data.getDate());
}
