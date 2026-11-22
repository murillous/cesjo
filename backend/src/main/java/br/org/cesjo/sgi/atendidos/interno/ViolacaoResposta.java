package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.atendidos.AmbitoViolacao;
import br.org.cesjo.sgi.atendidos.TipoViolacao;

/** Violação registrada, sem a descrição: ler a descrição é auditado e tem rota própria (ADR 0145). */
record ViolacaoResposta(UUID id, UUID insercaoId, TipoViolacao tipo, AmbitoViolacao ambito,
		LocalDate dataIdentificacao) {
}
