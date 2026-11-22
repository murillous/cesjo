package br.org.cesjo.sgi.atendidos.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.atendidos.AmbitoViolacao;
import br.org.cesjo.sgi.atendidos.TipoViolacao;

/** Violação com a descrição. Cada entrega é registrada como leitura sensível (ADR 0145). */
record ViolacaoCompleta(UUID id, UUID pessoaId, TipoViolacao tipo, AmbitoViolacao ambito,
		LocalDate dataIdentificacao, String descricao, Instant registradaEm) {

	static ViolacaoCompleta de(ViolacaoDireito violacao) {
		return new ViolacaoCompleta(violacao.getId(), violacao.getPessoaId(), violacao.getTipo(),
				violacao.getAmbito(), violacao.getDataIdentificacao(), violacao.getDescricao(), violacao.getCriadoEm());
	}

}
