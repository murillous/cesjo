package br.org.cesjo.sgi.atendimento.interno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cancelamento da visita, sempre com motivo (ADR 0073). */
record CancelamentoVisita(@NotBlank(message = "informe o motivo do cancelamento") @Size(max = 500) String motivo) {
}
