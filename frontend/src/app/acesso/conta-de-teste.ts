import { Permissao } from './permissao';
import { ContaSessao } from './sessao';

/** Conta fictícia para os testes do frontend. */
export function contaDeTeste(mudancas: Partial<ContaSessao> = {}): ContaSessao {
  const permissoes: Permissao[] = [
    'CONTAS_GERENCIAR',
    'AUDITORIA_LER',
    'DOCUMENTOS_LER',
    'DOCUMENTOS_ENVIAR',
    'NOTIFICACOES_LER',
    'LISTA_ESPERA_LER',
    'LISTA_ESPERA_CADASTRAR',
    'LISTA_ESPERA_FILA_LER',
    'LISTA_ESPERA_EDITAR',
    'COLABORADORES_LER',
    'COLABORADORES_GERENCIAR',
    'VISITAS_LER',
    'VISITAS_AGENDAR',
  ];
  return {
    id: '00000000-0000-0000-0000-000000000001',
    login: 'coordenacao',
    nome: 'Pessoa Fictícia',
    perfil: 'COORDENADOR',
    permissoes,
    trocaSenhaObrigatoria: false,
    ...mudancas,
  };
}
