import { HttpErrorResponse } from '@angular/common/http';

/** Corpo `ProblemDetail` (RFC 9457) das respostas de erro da API. */
export interface Problema {
  title?: string;
  detail?: string;
  status?: number;
  erros?: readonly { campo: string; mensagem: string }[];
}

/** Título do 403 que o backend devolve enquanto a conta precisa trocar a senha (ADR 0010). */
export const TITULO_TROCA_SENHA_OBRIGATORIA = 'Troca de senha obrigatória';

/** Mensagem para a tela a partir de um erro HTTP, com o texto do `ProblemDetail` quando houver. */
export function mensagemDeErro(erro: unknown, padrao: string): string {
  if (erro instanceof HttpErrorResponse) {
    const problema = erro.error as Problema | null;
    if (problema?.erros?.length) {
      return problema.erros.map((e) => `${e.campo}: ${e.mensagem}`).join('. ');
    }
    if (problema?.detail) {
      return problema.detail;
    }
    if (erro.status === 0) {
      return 'Não foi possível falar com o servidor. Confira a conexão e tente de novo.';
    }
  }
  return padrao;
}
