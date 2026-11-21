package br.org.cesjo.sgi.listaespera;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de aplicação: uma vaga do serviço foi liberada, por exemplo por desligamento (RF003, RF008,
 * ADR 0111). Quem libera a vaga publica; a Lista de espera ouve para avisar a abertura de vaga (#31).
 *
 * <p>
 * O tipo fica na Lista de espera, e não em Atendidos, porque Atendidos já depende da Lista de espera
 * (ADR 0081): se a Lista de espera ouvisse um tipo de Atendidos, os dois módulos dependeriam um do
 * outro. O evento não leva dado pessoal, só ids e a data.
 *
 * @param insercaoId inserção que deixou de ocupar a vaga
 * @param data dia em que a vaga ficou livre
 */
public record VagaLiberada(UUID insercaoId, LocalDate data) {

	public VagaLiberada {
		Objects.requireNonNull(insercaoId, "insercaoId");
		Objects.requireNonNull(data, "data");
	}

}
