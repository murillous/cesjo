package br.org.cesjo.sgi.listaespera.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.listaespera.MotivoSaida;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;

/**
 * Pedido de mudança de situação (ADR 0091). As regras que dependem da situação atual ficam no serviço.
 *
 * @param motivoSaida obrigatório para {@code SAIU}
 * @param observacao obrigatória para o motivo {@code OUTRO} e para a volta à fila
 */
record MudancaSituacao(@NotNull SituacaoInscricao situacao,
		@NotNull @PastOrPresent(message = "a data não pode ser futura") LocalDate data, MotivoSaida motivoSaida,
		@Size(max = 1000) String observacao) {

	MudancaSituacao {
		observacao = observacao == null || observacao.isBlank() ? null : observacao.strip();
	}

}
