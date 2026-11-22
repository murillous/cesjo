package br.org.cesjo.sgi.institucional.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.institucional.TipoParceiro;

/**
 * Dados cadastrais do parceiro (RF013, ADR 0172). Só nome e tipo são obrigatórios. {@code ativo} é
 * ignorado no cadastro, em que o parceiro nasce ativo, e obrigatório na edição.
 *
 * <p>
 * PENDENTE-CESJO(P10): campos provisórios (ADR 0172).
 */
record DadosParceiro(@NotBlank @Size(max = 150) String nome, @NotNull TipoParceiro tipo,
		@Size(max = 200) String instrumento, LocalDate vigenciaInicio, LocalDate vigenciaFim,
		@Size(max = 500) String contato, @Size(max = 2000) String observacoes, Boolean ativo) {

	@AssertTrue(message = "o fim da vigência não pode ser antes do início")
	boolean isVigenciaCoerente() {
		return vigenciaInicio == null || vigenciaFim == null || !vigenciaFim.isBefore(vigenciaInicio);
	}

	/** Texto sem espaços nas pontas, ou nulo quando vazio. */
	static String texto(String valor) {
		return valor == null || valor.isBlank() ? null : valor.strip();
	}

}
