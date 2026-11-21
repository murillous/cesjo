import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { OrigemEncaminhamento, SituacaoInscricao, TipoDeficiencia } from './rotulos';

/** Dados do cadastro na lista de espera (RF001, ADR 0038). Datas no formato `AAAA-MM-DD`. */
export interface NovaInscricao {
  nomeCompleto: string;
  dataNascimento: string | null;
  tipoDeficiencia: TipoDeficiencia;
  telefone: string;
  nomeResponsavel: string;
  origemEncaminhamento: OrigemEncaminhamento;
  dataCadastro: string;
}

/** Inscrição como `/api/lista-espera/inscricoes` devolve (ADR 0037). */
export interface Inscricao {
  id: string;
  pessoaId: string;
  nomeCompleto: string;
  dataNascimento: string | null;
  tipoDeficiencia: TipoDeficiencia;
  telefone: string;
  nomeResponsavel: string;
  origemEncaminhamento: OrigemEncaminhamento;
  dataCadastro: string;
  situacao: SituacaoInscricao;
  criadoEm: string;
}

@Injectable({ providedIn: 'root' })
export class ListaEsperaApi {
  private readonly http = inject(HttpClient);

  inscrever(dados: NovaInscricao): Observable<Inscricao> {
    return this.http.post<Inscricao>('/api/lista-espera/inscricoes', dados);
  }

  obter(id: string): Observable<Inscricao> {
    return this.http.get<Inscricao>(`/api/lista-espera/inscricoes/${encodeURIComponent(id)}`);
  }
}
