package br.org.cesjo.sgi.atendimento.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Versão do PIA com o conteúdo decifrado. Só sai pela leitura auditada (ADR 0103).
 *
 * @param editavel se esta é a versão mais nova e está em elaboração
 * @param maisNova se esta é a versão mais nova, a única que muda de status ou gera versão nova
 */
record PiaResposta(UUID id, UUID pessoaId, String nomePessoa, int numeroVersao, StatusPia status,
		ConteudoPia conteudo, boolean editavel, boolean maisNova, UUID versaoAnteriorId, LocalDate apresentadoFamiliaEm,
		LocalDate apresentadoEquipeEm, LocalDate aprovadoEm, Instant criadoEm, Instant atualizadoEm) {
}
