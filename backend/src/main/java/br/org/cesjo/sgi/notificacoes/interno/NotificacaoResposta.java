package br.org.cesjo.sgi.notificacoes.interno;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Notificação como a conta logada a vê.
 *
 * @param lidaEm quando esta conta leu a notificação, ou nulo se ainda não leu
 */
record NotificacaoResposta(UUID id, String tipo, String mensagem, String referencia, Instant criadaEm,
		Instant lidaEm) {

	@JsonProperty("lida")
	public boolean lida() {
		return lidaEm != null;
	}

}
