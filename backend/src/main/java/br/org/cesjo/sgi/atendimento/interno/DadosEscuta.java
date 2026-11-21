package br.org.cesjo.sgi.atendimento.interno;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Campos da escuta (RF005, ADR 0100), no registro e na correção. Os quatro textos são opcionais um a um,
 * mas a escuta precisa ter pelo menos um.
 *
 * @param profissionalId colaborador responsável pela escuta
 */
record DadosEscuta(@NotNull @PastOrPresent(message = "a data da escuta não pode ser futura") LocalDate dataEscuta,
		@NotNull UUID profissionalId, @Size(max = 10_000) String dinamicaFamiliar,
		@Size(max = 10_000) String necessidadesImediatas, @Size(max = 10_000) String habilidades,
		@Size(max = 10_000) String potencialidades) {

	@AssertTrue(message = "preencha pelo menos um dos campos da escuta")
	boolean isComConteudo() {
		return Escuta.limpo(dinamicaFamiliar) != null || Escuta.limpo(necessidadesImediatas) != null
				|| Escuta.limpo(habilidades) != null || Escuta.limpo(potencialidades) != null;
	}

}
