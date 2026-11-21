package br.org.cesjo.sgi.atendidos.interno;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Desligamento do usuário atendido (RF008, ADR 0110). */
@RestController
class DesligamentoControlador {

	private final ServicoDesligamento servico;

	DesligamentoControlador(ServicoDesligamento servico) {
		this.servico = servico;
	}

	@PostMapping("/api/atendidos/{id}/desligamento")
	@PreAuthorize("hasAuthority('ATENDIDOS_DESLIGAR')")
	@ResponseStatus(HttpStatus.CREATED)
	DesligamentoResposta desligar(@PathVariable UUID id, @Valid @RequestBody NovoDesligamento novo) {
		return servico.desligar(id, novo);
	}

}
