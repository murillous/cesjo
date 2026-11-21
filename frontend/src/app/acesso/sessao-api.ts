import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';

import { ContaSessao } from './sessao';

/** Chamadas de `/api/sessao` (ADR 0010). */
@Injectable({ providedIn: 'root' })
export class SessaoApi {
  private readonly http = inject(HttpClient);

  /** Conta logada. Responde 401 quando não há sessão. */
  obter(): Observable<ContaSessao> {
    return this.http.get<ContaSessao>('/api/sessao');
  }

  /** Busca o token CSRF antes, para o cookie `XSRF-TOKEN` existir na hora do login. */
  entrar(login: string, senha: string): Observable<ContaSessao> {
    return this.http
      .get('/api/sessao/csrf')
      .pipe(switchMap(() => this.http.post<ContaSessao>('/api/sessao/entrar', { login, senha })));
  }

  sair(): Observable<void> {
    return this.http.post<void>('/api/sessao/sair', null);
  }

  trocarSenha(senhaAtual: string, novaSenha: string): Observable<ContaSessao> {
    return this.http.post<ContaSessao>('/api/sessao/senha', { senhaAtual, novaSenha });
  }
}
