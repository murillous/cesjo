import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { TITULO_TROCA_SENHA_OBRIGATORIA } from './problema';
import { Sessao } from './sessao';
import { ROTA_ENTRAR, ROTA_TROCAR_SENHA } from './sessao.guard';

/** Respostas 401 destas chamadas são tratadas por quem chamou, sem redirecionar. */
const SEM_REDIRECIONAMENTO = ['/api/sessao', '/api/sessao/entrar'];

/**
 * 401 da API (sessão expirada ou encerrada): limpa a sessão e leva para a tela de entrada, voltando
 * depois para onde a pessoa estava. 403 de troca de senha pendente: leva para a tela de troca.
 */
export const sessaoInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const sessao = inject(Sessao);
  const router = inject(Router);
  return proximo(requisicao).pipe(
    catchError((erro: unknown) => {
      if (erro instanceof HttpErrorResponse && !SEM_REDIRECIONAMENTO.includes(requisicao.url)) {
        if (erro.status === 401) {
          sessao.definir(null);
          const atual = router.url;
          void router.navigate([ROTA_ENTRAR], {
            queryParams:
              atual && !atual.startsWith(ROTA_ENTRAR) && atual !== '/' ? { retorno: atual } : {},
          });
        } else if (erro.status === 403 && erro.error?.title === TITULO_TROCA_SENHA_OBRIGATORIA) {
          void router.navigate([ROTA_TROCAR_SENHA]);
        }
      }
      return throwError(() => erro);
    }),
  );
};
