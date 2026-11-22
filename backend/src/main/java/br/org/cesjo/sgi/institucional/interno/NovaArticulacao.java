package br.org.cesjo.sgi.institucional.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** Ação de articulação já feita com o parceiro (RF013, ADR 0172). */
record NovaArticulacao(@NotNull @PastOrPresent(message = "a data não pode ser futura") LocalDate data,
		@NotBlank @Size(max = 2000) String descricao) {
}
