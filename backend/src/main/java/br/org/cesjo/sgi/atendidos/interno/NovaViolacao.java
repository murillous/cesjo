package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.atendidos.AmbitoViolacao;
import br.org.cesjo.sgi.atendidos.TipoViolacao;

/**
 * Dados do registro de violação de direitos (RF009, ADR 0112). O âmbito é obrigatório nas categorias
 * que o pedem e proibido nas outras.
 */
record NovaViolacao(@NotNull TipoViolacao tipo, AmbitoViolacao ambito,
		@NotNull @PastOrPresent(message = "a data de identificação não pode ser futura") LocalDate dataIdentificacao,
		@NotBlank @Size(max = 4000) String descricao) {

	@AssertTrue(message = "informe o âmbito, intra ou extrafamiliar, só para abuso sexual e violência física ou psicológica")
	boolean isAmbitoDaCategoria() {
		return tipo == null || tipo.exigeAmbito() == (ambito != null);
	}

}
