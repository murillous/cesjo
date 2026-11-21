import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Sessao } from './sessao';

export const ROTA_ENTRAR = '/entrar';
export const ROTA_TROCAR_SENHA = '/trocar-senha';

/**
 * Rotas internas: sem sessão, vai para a tela de entrada guardando o destino; com troca de senha
 * pendente, vai para a tela de troca.
 */
export const exigeSessao: CanActivateFn = (_rota, estado) => {
  const sessao = inject(Sessao);
  const router = inject(Router);
  const conta = sessao.conta();
  if (!conta) {
    return router.createUrlTree([ROTA_ENTRAR], {
      queryParams: estado.url && estado.url !== '/' ? { retorno: estado.url } : {},
    });
  }
  if (conta.trocaSenhaObrigatoria) {
    return router.createUrlTree([ROTA_TROCAR_SENHA]);
  }
  return true;
};

/** Tela de troca de senha: exige sessão, mas não exige a troca já feita. */
export const exigeSessaoParaTrocarSenha: CanActivateFn = () =>
  inject(Sessao).autenticada() || inject(Router).createUrlTree([ROTA_ENTRAR]);

/** Tela de entrada: quem já está logado vai para o início. */
export const apenasSemSessao: CanActivateFn = () =>
  !inject(Sessao).autenticada() || inject(Router).createUrlTree(['/']);
