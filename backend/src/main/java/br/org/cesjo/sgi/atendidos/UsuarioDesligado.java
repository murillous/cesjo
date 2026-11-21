package br.org.cesjo.sgi.atendidos;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de aplicação: um usuário atendido foi desligado e a vaga dele ficou livre (RF003, RF008,
 * ADR 0111). Publicado dentro da transação do desligamento. Não leva dado pessoal, só ids e a data.
 *
 * <p>
 * A Lista de espera não pode ouvir este tipo, porque Atendidos já depende dela (ADR 0081). Quem reage
 * à vaga (a notificação da #31) ouve aqui em Atendidos e chama a API da Lista de espera.
 *
 * @param insercaoId inserção que deixou de ocupar a vaga
 * @param data dia do desligamento
 */
public record UsuarioDesligado(UUID insercaoId, LocalDate data) {

	public UsuarioDesligado {
		Objects.requireNonNull(insercaoId, "insercaoId");
		Objects.requireNonNull(data, "data");
	}

}
