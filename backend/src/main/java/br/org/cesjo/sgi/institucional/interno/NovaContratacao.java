package br.org.cesjo.sgi.institucional.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

/** Contratação do colaborador, que abre um vínculo (RF014, ADR 0170). */
record NovaContratacao(
		@NotNull @PastOrPresent(message = "a data de contratação não pode ser futura") LocalDate dataContratacao) {
}
