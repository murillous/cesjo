package br.org.cesjo.sgi.atendimento.interno;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Agendamento de visita domiciliar (RF004, ADR 0070).
 *
 * @param pessoaId pessoa com inscrição ativa na lista de espera
 * @param dataHora data e hora locais do Centro-Dia, por exemplo {@code 2026-11-25T14:30}
 * @param profissionais um ou mais colaboradores ativos
 */
record NovaVisita(@NotNull UUID pessoaId, @NotNull LocalDateTime dataHora,
		@NotEmpty(message = "escolha pelo menos um profissional") List<@NotNull UUID> profissionais) {
}
