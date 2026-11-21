package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.atendidos.Turno;

/**
 * Dados da inserção no serviço a partir da fila (RF007, ADR 0080). O cuidador familiar responsável é um
 * responsável já cadastrado da pessoa ({@code cuidadorResponsavelId}) ou um novo
 * ({@code nomeNovoCuidador}), nunca os dois.
 */
record NovaInsercao(@NotNull UUID inscricaoId,
		@NotNull @PastOrPresent(message = "a data de inserção não pode ser futura") LocalDate dataInsercao,
		@NotNull Turno turno, UUID cuidadorResponsavelId, @Size(max = 150) String nomeNovoCuidador) {

	// PENDENTE-CESJO(P7): um cuidador por inserção, escolhido entre os responsáveis da pessoa ou
	// cadastrado na hora (ADR 0080).
	@AssertTrue(message = "escolha um responsável já cadastrado ou informe o nome de um novo cuidador")
	boolean isCuidador() {
		boolean temNovo = nomeNovoCuidador != null && !nomeNovoCuidador.isBlank();
		return (cuidadorResponsavelId != null) != temNovo;
	}

}
