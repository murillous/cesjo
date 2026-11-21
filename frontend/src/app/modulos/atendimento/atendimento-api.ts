import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Profissional } from '../visitas/visitas-api';

export interface PessoaAtendida {
  id: string;
  nomeCompleto: string;
}

/** Os quatro textos do RF005, a data e o profissional responsável (ADR 0100). */
export interface DadosEscuta {
  dataEscuta: string;
  profissionalId: string;
  dinamicaFamiliar: string | null;
  necessidadesImediatas: string | null;
  habilidades: string | null;
  potencialidades: string | null;
}

export interface Escuta extends Omit<DadosEscuta, 'profissionalId'> {
  id: string;
  pessoaId: string;
  nomePessoa: string;
  profissional: Profissional;
  criadoEm: string;
  atualizadoEm: string;
}

/** Escuta na lista da pessoa, sem os textos. */
export interface ResumoEscuta {
  id: string;
  pessoaId: string;
  dataEscuta: string;
  profissional: Profissional;
}

export type StatusPia = 'ELABORADO' | 'APRESENTADO_FAMILIA' | 'APRESENTADO_EQUIPE' | 'APROVADO';

/** Objetivo do PIA. Estrutura provisória (P5, ADR 0103). */
export interface ObjetivoPia {
  descricao: string;
  metas: string | null;
  acoes: string | null;
  /** `AAAA-MM-DD`. */
  prazo: string | null;
  responsavel: string | null;
}

export interface ConteudoPia {
  diagnostico: string | null;
  objetivos: ObjetivoPia[];
  observacoes: string | null;
}

/** Versão do PIA na lista da pessoa, sem o conteúdo. */
export interface ResumoPia {
  id: string;
  pessoaId: string;
  numeroVersao: number;
  status: StatusPia;
  criadoEm: string;
  atualizadoEm: string;
}

export interface Pia extends ResumoPia {
  nomePessoa: string;
  conteudo: ConteudoPia;
  /** Versão mais nova e em elaboração. */
  editavel: boolean;
  /** Versão mais nova: a única que muda de status ou gera versão nova (ADR 0106). */
  maisNova: boolean;
  versaoAnteriorId: string | null;
  apresentadoFamiliaEm: string | null;
  apresentadoEquipeEm: string | null;
  aprovadoEm: string | null;
}

export type PassoPia = 'apresentar-a-familia' | 'apresentar-a-equipe' | 'aprovar';

@Injectable({ providedIn: 'root' })
export class AtendimentoApi {
  private readonly http = inject(HttpClient);

  pessoas(): Observable<PessoaAtendida[]> {
    return this.http.get<PessoaAtendida[]>('/api/atendimento/pessoas');
  }

  pessoa(id: string): Observable<PessoaAtendida> {
    return this.http.get<PessoaAtendida>(`/api/atendimento/pessoas/${encodeURIComponent(id)}`);
  }

  escutas(pessoaId: string): Observable<ResumoEscuta[]> {
    return this.http.get<ResumoEscuta[]>('/api/escutas', {
      params: new HttpParams().set('pessoaId', pessoaId),
    });
  }

  /** Escuta completa. Cada leitura fica no log de auditoria. */
  escuta(id: string): Observable<Escuta> {
    return this.http.get<Escuta>(`/api/escutas/${encodeURIComponent(id)}`);
  }

  registrarEscuta(pessoaId: string, dados: DadosEscuta): Observable<Escuta> {
    return this.http.post<Escuta>('/api/escutas', { pessoaId, dados });
  }

  corrigirEscuta(id: string, dados: DadosEscuta): Observable<Escuta> {
    return this.http.put<Escuta>(`/api/escutas/${encodeURIComponent(id)}`, dados);
  }

  pias(pessoaId: string): Observable<ResumoPia[]> {
    return this.http.get<ResumoPia[]>('/api/pias', {
      params: new HttpParams().set('pessoaId', pessoaId),
    });
  }

  /** Versão do PIA com o conteúdo. Cada leitura fica no log de auditoria. */
  pia(id: string): Observable<Pia> {
    return this.http.get<Pia>(`/api/pias/${encodeURIComponent(id)}`);
  }

  elaborarPia(pessoaId: string, conteudo: ConteudoPia): Observable<Pia> {
    return this.http.post<Pia>('/api/pias', { pessoaId, conteudo });
  }

  editarPia(id: string, conteudo: ConteudoPia): Observable<Pia> {
    return this.http.put<Pia>(`/api/pias/${encodeURIComponent(id)}`, conteudo);
  }

  /** Próximo passo do PIA, com a data `AAAA-MM-DD` em que aconteceu (ADR 0106). */
  avancarPia(id: string, passo: PassoPia, data: string): Observable<Pia> {
    return this.http.post<Pia>(`/api/pias/${encodeURIComponent(id)}/${passo}`, { data });
  }

  /** Versão nova em elaboração, com o conteúdo desta. */
  novaVersaoPia(id: string): Observable<Pia> {
    return this.http.post<Pia>(`/api/pias/${encodeURIComponent(id)}/nova-versao`, null);
  }

  profissionaisDaEscuta(): Observable<{
    profissionais: Profissional[];
    sugeridoId: string | null;
  }> {
    return this.http.get<{ profissionais: Profissional[]; sugeridoId: string | null }>(
      '/api/escutas/profissionais',
    );
  }
}
