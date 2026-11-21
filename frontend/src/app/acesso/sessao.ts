import { computed, Injectable, signal } from '@angular/core';

import { Perfil, Permissao } from './permissao';

/** Conta autenticada, como devolvida por `GET /api/sessao`. */
export interface ContaSessao {
  id: string;
  login: string;
  nome: string;
  perfil: Perfil;
  permissoes: readonly Permissao[];
  /** Verdadeiro enquanto a conta precisa trocar a senha (primeiro acesso ou senha redefinida). */
  trocaSenhaObrigatoria: boolean;
}

/**
 * Estado da sessão no frontend. Os outros módulos só leem `conta` e `temPermissao`; carregar,
 * entrar e sair é responsabilidade do módulo de acesso (issue #13).
 */
@Injectable({ providedIn: 'root' })
export class Sessao {
  private readonly contaAtual = signal<ContaSessao | null>(null);

  readonly conta = this.contaAtual.asReadonly();
  readonly autenticada = computed(() => this.contaAtual() !== null);

  temPermissao(permissao: Permissao): boolean {
    return this.contaAtual()?.permissoes.includes(permissao) ?? false;
  }

  /** Define a conta da sessão. Usado pelo login, pelo carregamento inicial e pelos testes. */
  definir(conta: ContaSessao | null): void {
    this.contaAtual.set(conta);
  }
}
