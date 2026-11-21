package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.atendidos.MotivoDesligamento;

/**
 * Dados do desligamento (RF008, ADR 0110). A observação é opcional, menos no motivo
 * {@link MotivoDesligamento#OUTROS}, que precisa dizer qual é o motivo.
 */
record NovoDesligamento(@NotNull @PastOrPresent(message = "a data de desligamento não pode ser futura") LocalDate data,
		@NotNull MotivoDesligamento motivo, @Size(max = 2000) String observacao) {

	static final int TAMANHO_MAXIMO_OBSERVACAO = 2000;

	@AssertTrue(message = "explique o motivo na observação quando o motivo for Outros")
	boolean isObservacaoDoMotivo() {
		return motivo != MotivoDesligamento.OUTROS || (observacao != null && !observacao.isBlank());
	}

	/** Observação sem espaços nas pontas, ou nula quando vazia. */
	String observacaoNormalizada() {
		return observacao == null || observacao.isBlank() ? null : observacao.strip();
	}

}
