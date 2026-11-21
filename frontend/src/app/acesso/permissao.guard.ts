import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';

import { Permissao } from './permissao';
import { Sessao } from './sessao';

/** Guard de rota: só deixa entrar quem tem a permissão. Ex.: `canMatch: [exigePermissao('AUDITORIA_LER')]`. */
export function exigePermissao(permissao: Permissao): CanMatchFn {
  return () => inject(Sessao).temPermissao(permissao);
}
