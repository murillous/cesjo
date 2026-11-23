import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { LinhaRelatorio } from './estatisticas-api';

/** Uma violação do caso, sem a descrição (ADR 0205). */
export interface ViolacaoDoCaso {
  categoria: string;
  /** "Intrafamiliar" ou "Extrafamiliar"; `null` nas categorias sem âmbito. */
  ambito: string | null;
  identificadaEm: string;
}

/** Um usuário com violação identificada no período e as intervenções do serviço. */
export interface CasoIntervencao {
  usuario: string;
  violacoes: ViolacaoDoCaso[];
  atendimentos: number;
  ultimoAtendimento: string | null;
  situacao: string;
}

/** Relatório de intervenção (CREAS) de um período (RF016, ADR 0205). */
export interface RelatorioIntervencao {
  titulo: string;
  inicio: string;
  fim: string;
  geradoEm: string;
  resumo: LinhaRelatorio[];
  casos: CasoIntervencao[];
}

/** Endpoint da exportação do relatório de intervenção, para o `app-botao-exportar`. */
export const URL_EXPORTACAO_INTERVENCAO = '/api/estatisticas/relatorios/intervencao/exportacao';

/** Relatório de intervenção do módulo `estatisticas` do backend. */
@Injectable({ providedIn: 'root' })
export class RelatorioIntervencaoApi {
  private readonly http = inject(HttpClient);

  /** Datas no formato `AAAA-MM-DD`, as duas inclusive. O servidor registra a leitura na auditoria. */
  gerar(inicio: string, fim: string): Observable<RelatorioIntervencao> {
    const params = new HttpParams().set('inicio', inicio).set('fim', fim);
    return this.http.get<RelatorioIntervencao>('/api/estatisticas/relatorios/intervencao', {
      params,
    });
  }
}
