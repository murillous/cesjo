import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { SituacaoInscricao, TipoDeficiencia } from '../lista-espera/rotulos';
import { SituacaoInsercao, Turno } from './rotulos';

/** Candidato da fila como `GET /api/atendidos/candidatos/{inscricaoId}` devolve (ADR 0080). */
export interface CandidatoInsercao {
  inscricaoId: string;
  pessoaId: string;
  nomeCompleto: string;
  tipoDeficiencia: TipoDeficiencia;
  dataCadastro: string;
  situacao: SituacaoInscricao;
  responsaveis: readonly { id: string; nomeCompleto: string; principal: boolean }[];
}

/**
 * Dados da inserção. O cuidador é um responsável já cadastrado (`cuidadorResponsavelId`) ou um novo
 * (`nomeNovoCuidador`), nunca os dois.
 */
export interface NovaInsercao {
  inscricaoId: string;
  dataInsercao: string;
  turno: Turno;
  cuidadorResponsavelId: string | null;
  nomeNovoCuidador: string | null;
}

export interface InsercaoCriada {
  id: string;
  pessoaId: string;
  inscricaoId: string;
  nomeCompleto: string;
  dataInsercao: string;
  turno: Turno;
  cuidadorResponsavelId: string;
  nomeCuidador: string;
  situacao: SituacaoInsercao;
}

@Injectable({ providedIn: 'root' })
export class AtendidosApi {
  private readonly http = inject(HttpClient);

  candidato(inscricaoId: string): Observable<CandidatoInsercao> {
    return this.http.get<CandidatoInsercao>(
      `/api/atendidos/candidatos/${encodeURIComponent(inscricaoId)}`,
    );
  }

  inserir(dados: NovaInsercao): Observable<InsercaoCriada> {
    return this.http.post<InsercaoCriada>('/api/atendidos/insercoes', dados);
  }
}
