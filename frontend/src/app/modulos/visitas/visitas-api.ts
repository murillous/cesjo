import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { SituacaoInscricao } from '../lista-espera/rotulos';

export type StatusVisita = 'AGENDADA' | 'REALIZADA' | 'CANCELADA';

export interface Profissional {
  id: string;
  nome: string;
  funcao: 'ASSISTENTE_SOCIAL' | 'PSICOLOGO' | 'TERAPEUTA_OCUPACIONAL' | 'OUTRA';
}

/** Visita como `/api/visitas` devolve (ADR 0070). `dataHora` é a hora local, `AAAA-MM-DDTHH:mm:ss`. */
export interface Visita {
  id: string;
  pessoaId: string;
  nomePessoa: string;
  dataHora: string;
  status: StatusVisita;
  profissionais: Profissional[];
  criadoEm: string;
}

/** Candidato da lista de espera que pode receber visita (ADR 0049). */
export interface Candidato {
  inscricaoId: string;
  pessoaId: string;
  nomeCompleto: string;
  dataCadastro: string;
  situacao: SituacaoInscricao;
}

export interface NovaVisita {
  pessoaId: string;
  /** Hora local, `AAAA-MM-DDTHH:mm`. */
  dataHora: string;
  profissionais: string[];
}

@Injectable({ providedIn: 'root' })
export class VisitasApi {
  private readonly http = inject(HttpClient);

  /** Agenda de `de` a `ate`, datas `AAAA-MM-DD` inclusive. */
  agenda(de: string, ate: string): Observable<Visita[]> {
    const params = new HttpParams().set('de', de).set('ate', ate);
    return this.http.get<Visita[]>('/api/visitas', { params });
  }

  obter(id: string): Observable<Visita> {
    return this.http.get<Visita>(`/api/visitas/${encodeURIComponent(id)}`);
  }

  agendar(dados: NovaVisita): Observable<Visita> {
    return this.http.post<Visita>('/api/visitas', dados);
  }

  candidatos(): Observable<Candidato[]> {
    return this.http.get<Candidato[]>('/api/visitas/candidatos');
  }

  profissionais(): Observable<Profissional[]> {
    return this.http.get<Profissional[]>('/api/visitas/profissionais');
  }
}
