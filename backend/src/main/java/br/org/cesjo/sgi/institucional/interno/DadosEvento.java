package br.org.cesjo.sgi.institucional.interno;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.institucional.DirecaoEvento;
import br.org.cesjo.sgi.institucional.TipoEventoInstitucional;

/**
 * Dados do registro e da edição de reunião ou evento institucional (RF012, ADR 0175).
 *
 * @param direcao obrigatória só para visitas (realizada ou recebida) e eventos (realizado ou
 * participado)
 * @param participantes quem participou, em texto livre
 * @param parceiroId parceiro ligado, opcional (ADR 0172)
 */
record DadosEvento(@NotNull TipoEventoInstitucional tipo, DirecaoEvento direcao,
		@NotNull @PastOrPresent(message = "a data não pode ser futura") LocalDate data,
		@NotBlank @Size(max = 4000) String descricao, @Size(max = 2000) String participantes, UUID parceiroId) {

	@AssertTrue(message = "visitas são realizadas ou recebidas, eventos são realizados ou participados e os demais tipos não têm indicação")
	boolean isDirecaoDoTipo() {
		return tipo == null || tipo.aceita(direcao);
	}

	String participantesNormalizados() {
		return participantes == null || participantes.isBlank() ? null : participantes.strip();
	}

}
