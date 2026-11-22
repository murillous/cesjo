import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Uma barra de gráfico: quantos caem numa categoria. `chave` é o código da categoria ou
 * `SEM_VALOR` para os sem categoria, como gênero não informado.
 */
export interface Contagem {
  chave: string;
  rotulo: string;
  total: number;
}

/** Dashboard de perfil dos usuários atendidos (RF017, ADR 0195). */
export interface PainelPerfil {
  geradoEm: string;
  totalAtivos: number;
  porTipoDeficiencia: Contagem[];
  porGenero: Contagem[];
  porFaixaEtaria: Contagem[];
  porTurno: Contagem[];
}

/** Desligamentos de um motivo e a taxa sobre os usuários atendidos no período, de 0 a 1. */
export interface TaxaDesligamento extends Contagem {
  taxa: number;
}

/** Dashboard de violações, atividades e desligamentos num período (RF017, ADR 0198). */
export interface PainelAtendimento {
  geradoEm: string;
  inicio: string;
  fim: string;
  /** `null` para quem não lê violações de direitos (ADR 0199). */
  violacoesPorCategoria: Contagem[] | null;
  atividadesPorTipo: Contagem[];
  totalAtividades: number;
  atendidosNoPeriodo: number;
  totalDesligamentos: number;
  taxaDesligamento: number;
  desligamentosPorMotivo: TaxaDesligamento[];
}

/** Uma linha do relatório estatístico (ADR 0200). */
export interface LinhaRelatorio {
  grupo: string;
  indicador: string;
  quantidade: number;
}

/** Relatório estatístico de um período (RF016, ADR 0200). */
export interface RelatorioEstatistico {
  titulo: string;
  inicio: string;
  fim: string;
  geradoEm: string;
  linhas: LinhaRelatorio[];
}

/** Endpoint da exportação do relatório mensal, para o `app-botao-exportar`. */
export const URL_EXPORTACAO_MENSAL = '/api/estatisticas/relatorios/mensal/exportacao';

/** Dashboards e relatórios estatísticos do módulo `estatisticas` do backend. */
@Injectable({ providedIn: 'root' })
export class EstatisticasApi {
  private readonly http = inject(HttpClient);

  /** Recalculado no servidor a cada chamada, sem cache. */
  perfil(): Observable<PainelPerfil> {
    return this.http.get<PainelPerfil>('/api/estatisticas/dashboards/perfil');
  }

  /** Datas no formato `AAAA-MM-DD`, as duas inclusive. */
  atendimento(inicio: string, fim: string): Observable<PainelAtendimento> {
    const params = new HttpParams().set('inicio', inicio).set('fim', fim);
    return this.http.get<PainelAtendimento>('/api/estatisticas/dashboards/atendimento', { params });
  }

  /** `mes` no formato `AAAA-MM`. */
  mensal(mes: string): Observable<RelatorioEstatistico> {
    const params = new HttpParams().set('mes', mes);
    return this.http.get<RelatorioEstatistico>('/api/estatisticas/relatorios/mensal', { params });
  }
}
