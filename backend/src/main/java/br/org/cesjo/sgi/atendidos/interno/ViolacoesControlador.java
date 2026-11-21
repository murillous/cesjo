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

/** Violações de direitos dos usuários atendidos (RF009, ADR 0112). */
@RestController
class ViolacoesControlador {

	private final ServicoViolacoes servico;

	ViolacoesControlador(ServicoViolacoes servico) {
		this.servico = servico;
	}

	@PostMapping("/api/atendidos/{id}/violacoes")
	@PreAuthorize("hasAuthority('VIOLACOES_REGISTRAR')")
	@ResponseStatus(HttpStatus.CREATED)
	ViolacaoResposta registrar(@PathVariable UUID id, @Valid @RequestBody NovaViolacao nova) {
		return servico.registrar(id, nova);
	}

}
