package br.org.cesjo.sgi.institucional.interno;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.institucional.DirecaoDoacao;
import br.org.cesjo.sgi.institucional.TipoDoacao;

/** Doação como a API devolve (ADR 0178). */
record DoacaoResposta(UUID id, DirecaoDoacao direcao, LocalDate data, String contraparte, TipoDoacao tipo,
		String descricao, String quantidade, BigDecimal valor, Instant criadoEm, Instant atualizadoEm) {

	static DoacaoResposta de(Doacao doacao) {
		return new DoacaoResposta(doacao.getId(), doacao.getDirecao(), doacao.getData(), doacao.getContraparte(),
				doacao.getTipo(), doacao.getDescricao(), doacao.getQuantidade(), doacao.getValor(),
				doacao.getCriadoEm(), doacao.getAtualizadoEm());
	}

}
