import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import {
  ArquivoBaixado,
  nomeDoContentDisposition,
} from '../../compartilhado/arquivos/arquivos-api';
import {
  MotivoSaida,
  OrigemEncaminhamento,
  SituacaoInscricao,
  TipoDeficiencia,
  TipoDocumento,
} from './rotulos';

/** Dados do cadastro na lista de espera (RF001, ADR 0038). Datas no formato `AAAA-MM-DD`. */
export interface NovaInscricao {
  nomeCompleto: string;
  dataNascimento: string | null;
  tipoDeficiencia: TipoDeficiencia;
  telefone: string;
  nomeResponsavel: string;
  origemEncaminhamento: OrigemEncaminhamento;
  dataCadastro: string;
  /** Opcionais: ids devolvidos por `POST /api/arquivos` (ADR 0056). */
  laudoMedicoArquivoId?: string | null;
  comprovanteResidenciaArquivoId?: string | null;
}

/** Edição dos dados do candidato (ADR 0090): os campos do cadastro, sem os anexos. */
export type EdicaoInscricao = Omit<
  NovaInscricao,
  'laudoMedicoArquivoId' | 'comprovanteResidenciaArquivoId'
>;

/** Pedido de mudança de situação (ADR 0091). */
export interface MudancaSituacao {
  situacao: SituacaoInscricao;
  data: string;
  motivoSaida?: MotivoSaida | null;
  observacao?: string | null;
}

/** Uma mudança de situação no histórico da ficha. */
export interface HistoricoSituacao {
  situacaoAnterior: SituacaoInscricao | null;
  situacaoNova: SituacaoInscricao;
  data: string;
  motivoSaida: MotivoSaida | null;
  observacao: string | null;
  registradoEm: string;
}

/** Documento anexado à inscrição, sem o conteúdo. */
export interface DocumentoInscricao {
  tipo: TipoDocumento;
  nomeArquivo: string;
  tipoMime: string;
  tamanho: number;
  anexadoEm: string;
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
  documentos: DocumentoInscricao[];
  /** Documentos obrigatórios que faltam; vazio quando o cadastro está completo. */
  documentosPendentes: TipoDocumento[];
  cadastroCompleto: boolean;
  /** Ausente quando a inscrição não está aguardando. */
  posicao?: number | null;
  dataSaida?: string | null;
  motivoSaida?: MotivoSaida | null;
  historico: HistoricoSituacao[];
}

/**
 * Uma linha da fila (`GET /api/lista-espera/inscricoes`). Para o perfil Consulta, que só vê os dados
 * básicos, `tipoDeficiencia`, `origemEncaminhamento` e `cadastroCompleto` não vêm (ADR 0058).
 */
export interface ItemFila {
  id: string;
  /** Ausente quando a inscrição não está aguardando. */
  posicao?: number;
  nomeCompleto: string;
  situacao: SituacaoInscricao;
  dataCadastro: string;
  tipoDeficiencia?: TipoDeficiencia;
  origemEncaminhamento?: OrigemEncaminhamento;
  cadastroCompleto?: boolean;
}

export interface PaginaFila {
  content: readonly ItemFila[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

/** Filtros da fila. Tipo de deficiência e origem só para quem tem `LISTA_ESPERA_LER`. */
export interface FiltrosFila {
  nome?: string | null;
  tipoDeficiencia?: TipoDeficiencia | null;
  origem?: OrigemEncaminhamento | null;
  situacao?: SituacaoInscricao | null;
}

@Injectable({ providedIn: 'root' })
export class ListaEsperaApi {
  private readonly http = inject(HttpClient);

  /** Fila em ordem, com a posição de cada candidato aguardando (RF002). */
  listar(filtros: FiltrosFila, pagina: number, tamanho: number): Observable<PaginaFila> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho);
    for (const [campo, valor] of Object.entries(filtros)) {
      const texto = typeof valor === 'string' ? valor.trim() : '';
      if (texto) {
        params = params.set(campo, texto);
      }
    }
    return this.http.get<PaginaFila>('/api/lista-espera/inscricoes', { params });
  }

  inscrever(dados: NovaInscricao): Observable<Inscricao> {
    return this.http.post<Inscricao>('/api/lista-espera/inscricoes', dados);
  }

  obter(id: string): Observable<Inscricao> {
    return this.http.get<Inscricao>(urlInscricao(id));
  }

  editar(id: string, dados: EdicaoInscricao): Observable<Inscricao> {
    return this.http.put<Inscricao>(urlInscricao(id), dados);
  }

  mudarSituacao(id: string, mudanca: MudancaSituacao): Observable<Inscricao> {
    return this.http.post<Inscricao>(`${urlInscricao(id)}/situacao`, mudanca);
  }

  /** Liga um arquivo já enviado à inscrição, substituindo o documento do mesmo tipo. */
  anexar(id: string, tipo: TipoDocumento, arquivoId: string): Observable<Inscricao> {
    return this.http.put<Inscricao>(`${urlInscricao(id)}/documentos/${tipo}`, { arquivoId });
  }

  /** Baixa o documento. O backend registra a leitura no log de auditoria. */
  baixarDocumento(id: string, tipo: TipoDocumento): Observable<ArquivoBaixado> {
    return this.http
      .get(`${urlInscricao(id)}/documentos/${tipo}`, { observe: 'response', responseType: 'blob' })
      .pipe(
        map((resposta) => ({
          nome:
            nomeDoContentDisposition(resposta.headers.get('Content-Disposition')) ?? 'documento',
          conteudo: resposta.body ?? new Blob(),
        })),
      );
  }
}

function urlInscricao(id: string): string {
  return `/api/lista-espera/inscricoes/${encodeURIComponent(id)}`;
}
