import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Uma linha do relatório de transparência: quantidade ou soma em reais (ADR 0210). */
export interface LinhaTransparencia {
  grupo: string;
  indicador: string;
  valor: number;
  unidade: 'QUANTIDADE' | 'REAIS';
}

/** Relatório de transparência de um período (RF016, ADR 0210). Só números agregados. */
export interface RelatorioTransparencia {
  titulo: string;
  inicio: string;
  fim: string;
  geradoEm: string;
  linhas: LinhaTransparencia[];
}

/** Endpoint da exportação do relatório de transparência, para o `app-botao-exportar`. */
export const URL_EXPORTACAO_TRANSPARENCIA = '/api/estatisticas/relatorios/transparencia/exportacao';

/** Relatório de transparência do módulo `estatisticas` do backend. */
@Injectable({ providedIn: 'root' })
export class TransparenciaApi {
  private readonly http = inject(HttpClient);

  /** Datas no formato `AAAA-MM-DD`, as duas inclusive. */
  gerar(inicio: string, fim: string): Observable<RelatorioTransparencia> {
    const params = new HttpParams().set('inicio', inicio).set('fim', fim);
    return this.http.get<RelatorioTransparencia>('/api/estatisticas/relatorios/transparencia', {
      params,
    });
  }
}

/** Período padrão do relatório de transparência: o ano anterior inteiro, que já fechou. */
export function anoAnterior(hoje: Date = new Date()): { inicio: Date; fim: Date } {
  const ano = hoje.getFullYear() - 1;
  return { inicio: new Date(ano, 0, 1), fim: new Date(ano, 11, 31) };
}
