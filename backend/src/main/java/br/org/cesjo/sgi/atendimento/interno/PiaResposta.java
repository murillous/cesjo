package br.org.cesjo.sgi.atendimento.interno;

import java.time.Instant;
import java.util.UUID;

/**
 * Versão do PIA com o conteúdo decifrado. Só sai pela leitura auditada (ADR 0103).
 *
 * @param editavel se esta é a versão mais nova e está em elaboração
 */
record PiaResposta(UUID id, UUID pessoaId, String nomePessoa, int numeroVersao, StatusPia status,
		ConteudoPia conteudo, boolean editavel, UUID versaoAnteriorId, Instant criadoEm, Instant atualizadoEm) {
}
