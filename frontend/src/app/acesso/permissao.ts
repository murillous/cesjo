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
  | 'LISTA_ESPERA_EXPORTAR'
  | 'CAPACIDADE_LER'
  | 'CAPACIDADE_EDITAR'
  | 'COLABORADORES_LER'
  | 'COLABORADORES_GERENCIAR'
  | 'ATENDIDOS_INSERIR'
  | 'ATENDIDOS_LER'
  | 'ATENDIDOS_LER_BASICO'
  | 'ATENDIDOS_EDITAR'
  | 'ATENDIDOS_DADOS_SENSIVEIS'
  | 'VISITAS_LER'
  | 'VISITAS_AGENDAR'
  | 'VISITAS_REGISTRAR'
  | 'ESCUTA_LER'
  | 'ESCUTA_REGISTRAR'
  | 'PIA_LER'
  | 'PIA_EDITAR'
  | 'PIA_APROVAR'
  | 'ATENDIDOS_DESLIGAR'
  | 'VIOLACOES_REGISTRAR'
  | 'VIOLACOES_LER'
  | 'ATIVIDADES_LER'
  | 'ATIVIDADES_REGISTRAR';

/** Perfis da seção 4 do documento de requisitos, com os nomes do enum `Perfil` do backend. */
export type Perfil = 'COORDENADOR' | 'EQUIPE_TECNICA' | 'ADMINISTRATIVO' | 'CONSULTA';
