package br.org.cesjo.sgi.institucional.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** Desligamento do colaborador, que fecha o vínculo aberto (RF014, ADR 0170). */
record NovoDesligamentoColaborador(
		@NotNull @PastOrPresent(message = "a data de desligamento não pode ser futura") LocalDate data,
		@NotBlank @Size(max = 2000) String motivo) {
}
