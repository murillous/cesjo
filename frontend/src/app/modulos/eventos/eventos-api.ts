import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Pagina } from '../contas/contas-api';
import { DirecaoEvento, TipoEvento } from './rotulos';

/** Reunião ou evento como `GET /api/eventos-institucionais` devolve (ADR 0175). */
export interface EventoInstitucional {
  id: string;
  tipo: TipoEvento;
  direcao: DirecaoEvento | null;
  /** `AAAA-MM-DD`. */
  data: string;
  descricao: string;
  participantes: string | null;
  parceiro: { id: string; nome: string } | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface DadosEvento {
  tipo: TipoEvento;
  direcao: DirecaoEvento | null;
  data: string;
  descricao: string;
  participantes: string | null;
  parceiroId: string | null;
}

/** Filtros da listagem; vazios não filtram. Datas em `AAAA-MM-DD`, inclusivas. */
export interface FiltroEventos {
  tipo: TipoEvento | null;
  de: string | null;
  ate: string | null;
}

@Injectable({ providedIn: 'root' })
export class EventosApi {
  private readonly http = inject(HttpClient);

  /** Do mais recente ao mais antigo. */
  listar(
    filtro: FiltroEventos,
    pagina: number,
    tamanho: number,
  ): Observable<Pagina<EventoInstitucional>> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho);
    for (const [chave, valor] of Object.entries(filtro)) {
      if (valor) {
        params = params.set(chave, valor as string);
      }
    }
    return this.http.get<Pagina<EventoInstitucional>>('/api/eventos-institucionais', { params });
  }

  obter(id: string): Observable<EventoInstitucional> {
    return this.http.get<EventoInstitucional>(
      `/api/eventos-institucionais/${encodeURIComponent(id)}`,
    );
  }

  registrar(dados: DadosEvento): Observable<EventoInstitucional> {
    return this.http.post<EventoInstitucional>('/api/eventos-institucionais', dados);
  }

  editar(id: string, dados: DadosEvento): Observable<EventoInstitucional> {
    return this.http.put<EventoInstitucional>(
      `/api/eventos-institucionais/${encodeURIComponent(id)}`,
      dados,
    );
  }
}
