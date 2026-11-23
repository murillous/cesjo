import { Injectable } from '@angular/core';

import { PaginadorPtBr } from '../../compartilhado/paginador-pt-br';

/**
 * Paginador do log de auditoria: quando o servidor só contou até o teto (ADR 0236), o rótulo diz
 * "de mais de N" em vez de um total que não é o real.
 */
@Injectable()
export class PaginadorAuditoria extends PaginadorPtBr {
  private totalExato = true;

  /** Atualiza o rótulo quando chega uma página nova. */
  definirTotalExato(exato: boolean): void {
    if (exato !== this.totalExato) {
      this.totalExato = exato;
      this.changes.next();
    }
  }

  override getRangeLabel = (pagina: number, tamanho: number, total: number): string => {
    if (total === 0) {
      return '0 de 0';
    }
    const inicio = pagina * tamanho + 1;
    const fim = Math.min((pagina + 1) * tamanho, total);
    const de = this.totalExato ? `${total}` : `mais de ${formatarNumero(total)}`;
    return `${inicio} a ${fim} de ${de}`;
  };
}

/** Número com separador de milhar, como no resto da tela. */
export function formatarNumero(valor: number): string {
  return valor.toLocaleString('pt-BR');
}
