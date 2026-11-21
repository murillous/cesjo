package br.org.cesjo.sgi.listaespera.interno;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Capacidade do serviço e painel de vagas (RF003, ADR 0092). */
@RestController
@RequestMapping("/api/lista-espera")
class VagasControlador {

	private final ServicoVagas servico;

	VagasControlador(ServicoVagas servico) {
		this.servico = servico;
	}

	@GetMapping("/vagas")
	@PreAuthorize("hasAuthority('CAPACIDADE_LER')")
	PainelVagas painel() {
		return servico.painel();
	}

	@PutMapping("/capacidade")
	@PreAuthorize("hasAuthority('CAPACIDADE_EDITAR')")
	PainelVagas definirCapacidade(@Valid @RequestBody NovaCapacidade nova) {
		return servico.definirCapacidade(nova.vagas());
	}

}
