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

  profissionaisDaEscuta(): Observable<{
    profissionais: Profissional[];
    sugeridoId: string | null;
  }> {
    return this.http.get<{ profissionais: Profissional[]; sugeridoId: string | null }>(
      '/api/escutas/profissionais',
    );
  }
}
