package br.org.cesjo.sgi.atendimento.interno;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Reagendamento: cancela a visita com o motivo e cria uma nova para a mesma pessoa (ADR 0073).
 *
 * @param dataHora data e hora locais da visita nova
 * @param profissionais profissionais da visita nova
 * @param motivo motivo do cancelamento da visita atual
 */
record ReagendamentoVisita(@NotNull LocalDateTime dataHora,
		@NotEmpty(message = "escolha pelo menos um profissional") List<@NotNull UUID> profissionais,
		@NotBlank(message = "informe o motivo do reagendamento") @Size(max = 500) String motivo) {
}
