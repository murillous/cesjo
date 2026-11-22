import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { OrigemEncaminhamento, SituacaoInscricao, TipoDeficiencia } from '../lista-espera/rotulos';
import {
  AmbitoViolacao,
  CrencaReligiosa,
  Escolaridade,
  FaixaEtaria,
  Genero,
  MotivoDesligamento,
  SituacaoInsercao,
  TipoViolacao,
  Turno,
} from './rotulos';

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

/**
 * Usuário atendido na visão básica (perfil Consulta e Administrativo): só nome, situação, turno e
 * datas (ADR 0082).
 */
export interface AtendidoBasico {
  completo: false;
  id: string;
  nomeCompleto: string;
  situacao: SituacaoInsercao;
  turno: Turno;
  dataInsercao: string;
  dataDesligamento: string | null;
}

/** Usuário atendido na visão completa (`ATENDIDOS_LER`). */
export interface AtendidoCompleto extends Omit<AtendidoBasico, 'completo'> {
  completo: true;
  pessoaId: string;
  tipoDeficiencia: TipoDeficiencia;
  dataNascimento: string | null;
  idade: number | null;
  faixaEtaria: FaixaEtaria | null;
  genero: Genero | null;
  bairro: string | null;
}

export type Atendido = AtendidoBasico | AtendidoCompleto;

/**
 * Dados pessoais completos (RF007, ADR 0140). Todos opcionais; CPF, NIS e CEP vêm só com os dígitos.
 * A lista de campos espelha o record `CadastroComplementar` do backend.
 */
export interface CadastroComplementar {
  genero: Genero | null;
  cpf: string | null;
  nis: string | null;
  rg: string | null;
  escolaridade: Escolaridade | null;
  cep: string | null;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  rendaFamiliar: number | null;
  recebeBpc: boolean;
  inscritoCadUnico: boolean;
  recebeBolsaFamilia: boolean;
}

/** Crença religiosa e saúde: só para quem tem `ATENDIDOS_DADOS_SENSIVEIS` (ADR 0142). */
export interface DadosSensiveis {
  crencaReligiosa: CrencaReligiosa | null;
  medicamentosUsoContinuo: string | null;
  observacoesSaude: string | null;
}

/** Corpo de `PUT /api/atendidos/{id}/cadastro`. Sem `dadosSensiveis`, crença e saúde ficam como estão. */
export interface EdicaoCadastro {
  nomeCompleto: string;
  dataNascimento: string | null;
  tipoDeficiencia: TipoDeficiencia;
  telefone: string;
  cadastro: CadastroComplementar;
  dadosSensiveis: DadosSensiveis | null;
}

/** Ficha completa: a inserção, a pessoa, o cadastro, o cuidador e a inscrição de origem (ADR 0083). */
export interface FichaCompleta extends AtendidoCompleto {
  telefone: string;
  nomeCuidador: string | null;
  inscricao: {
    id: string;
    dataCadastro: string;
    origemEncaminhamento: OrigemEncaminhamento;
  } | null;
  /** Ler a observação fica registrado no log de auditoria (ADR 0110). */
  desligamento: {
    id: string;
    data: string;
    motivo: MotivoDesligamento;
    observacao: string | null;
  } | null;
  cadastro: CadastroComplementar;
  /** Nulo para quem não tem `ATENDIDOS_DADOS_SENSIVEIS`; ler fica registrado na auditoria. */
  dadosSensiveis: DadosSensiveis | null;
}

/** Dados do desligamento (RF008, ADR 0110). Observação obrigatória no motivo `OUTROS`. */
export interface NovoDesligamento {
  data: string;
  motivo: MotivoDesligamento;
  observacao: string | null;
}

/** Dados do registro de violação de direitos (RF009, ADR 0112). */
export interface NovaViolacao {
  tipo: TipoViolacao;
  ambito: AmbitoViolacao | null;
  dataIdentificacao: string;
  descricao: string;
}

export type FichaAtendido = AtendidoBasico | FichaCompleta;

/**
 * Filtros da listagem. Tipo de deficiência, faixa etária, gênero e bairro só para quem tem
 * `ATENDIDOS_LER`.
 */
export interface FiltrosAtendidos {
  nome?: string | null;
  turno?: Turno | null;
  situacao?: SituacaoInsercao | null;
  tipoDeficiencia?: TipoDeficiencia | null;
  faixaEtaria?: FaixaEtaria | null;
  genero?: Genero | null;
  bairro?: string | null;
}

export interface PaginaAtendidos {
  content: readonly Atendido[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

@Injectable({ providedIn: 'root' })
export class AtendidosApi {
  private readonly http = inject(HttpClient);

  candidato(inscricaoId: string): Observable<CandidatoInsercao> {
    return this.http.get<CandidatoInsercao>(
      `/api/atendidos/candidatos/${encodeURIComponent(inscricaoId)}`,
    );
  }

  listar(filtros: FiltrosAtendidos, pagina: number, tamanho: number): Observable<PaginaAtendidos> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho);
    for (const [nome, valor] of Object.entries(filtros)) {
      const texto = typeof valor === 'string' ? valor.trim() : valor;
      if (texto) {
        params = params.set(nome, texto);
      }
    }
    return this.http.get<PaginaAtendidos>('/api/atendidos', { params });
  }

  ficha(id: string): Observable<FichaAtendido> {
    return this.http.get<FichaAtendido>(`/api/atendidos/${encodeURIComponent(id)}`);
  }

  desligar(id: string, dados: NovoDesligamento): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(
      `/api/atendidos/${encodeURIComponent(id)}/desligamento`,
      dados,
    );
  }

  registrarViolacao(id: string, dados: NovaViolacao): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(
      `/api/atendidos/${encodeURIComponent(id)}/violacoes`,
      dados,
    );
  }

  salvarCadastro(id: string, dados: EdicaoCadastro): Observable<void> {
    return this.http.put<void>(`/api/atendidos/${encodeURIComponent(id)}/cadastro`, dados);
  }

  inserir(dados: NovaInsercao): Observable<InsercaoCriada> {
    return this.http.post<InsercaoCriada>('/api/atendidos/insercoes', dados);
  }
}
