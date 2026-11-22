import { HttpClient } from '@angular/common/http';
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

/** Dashboards e relatórios estatísticos do módulo `estatisticas` do backend. */
@Injectable({ providedIn: 'root' })
export class EstatisticasApi {
  private readonly http = inject(HttpClient);

  /** Recalculado no servidor a cada chamada, sem cache. */
  perfil(): Observable<PainelPerfil> {
    return this.http.get<PainelPerfil>('/api/estatisticas/dashboards/perfil');
  }
}
