import { inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { Sessao } from './sessao';
import { SessaoApi } from './sessao-api';

/**
 * Carrega a sessão na inicialização do app (`provideAppInitializer`). Sem sessão, ou se a API não
 * responder, o app começa deslogado e o guard leva para a tela de entrada.
 */
export async function carregarSessao(): Promise<void> {
  const sessao = inject(Sessao);
  const api = inject(SessaoApi);
  try {
    sessao.definir(await firstValueFrom(api.obter()));
  } catch {
    sessao.definir(null);
  }
}
