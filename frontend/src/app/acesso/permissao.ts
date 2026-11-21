/**
 * Permissões do sistema, com os mesmos nomes do enum `Permissao` do backend
 * (`br.org.cesjo.sgi.acesso.Permissao`). O backend é a autoridade; aqui elas só decidem o que
 * aparece na tela (ADR 0007).
 */
export type Permissao =
  | 'CONTAS_GERENCIAR'
  | 'AUDITORIA_LER'
  | 'DOCUMENTOS_LER'
  | 'DOCUMENTOS_ENVIAR'
  | 'NOTIFICACOES_LER'
  | 'LISTA_ESPERA_LER'
  | 'LISTA_ESPERA_CADASTRAR'
  | 'LISTA_ESPERA_FILA_LER'
  | 'LISTA_ESPERA_EDITAR'
  // Do módulo atendidos (issue #39); o backend só concede depois daquela junção.
  | 'ATENDIDOS_INSERIR';

/** Perfis da seção 4 do documento de requisitos, com os nomes do enum `Perfil` do backend. */
export type Perfil = 'COORDENADOR' | 'EQUIPE_TECNICA' | 'ADMINISTRATIVO' | 'CONSULTA';
