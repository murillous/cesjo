package br.org.cesjo.sgi.institucional.interno;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Histórico de vínculos, contratações e desligamentos do colaborador (RF014, ADR 0170). */
@RestController
@RequestMapping("/api/colaboradores/{id}")
class VinculosControlador {

	private final ServicoVinculos servico;

	VinculosControlador(ServicoVinculos servico) {
		this.servico = servico;
	}

	/** Vínculos do mais recente ao mais antigo. */
	@GetMapping("/vinculos")
	@PreAuthorize("hasAuthority('COLABORADORES_LER')")
	List<VinculoResposta> listar(@PathVariable UUID id) {
		return servico.listar(id);
	}

	@PostMapping("/contratacoes")
	@PreAuthorize("hasAuthority('COLABORADORES_GERENCIAR')")
	@ResponseStatus(HttpStatus.CREATED)
	VinculoResposta contratar(@PathVariable UUID id, @Valid @RequestBody NovaContratacao nova) {
		return servico.contratar(id, nova.dataContratacao());
	}

	@PostMapping("/desligamento")
	@PreAuthorize("hasAuthority('COLABORADORES_GERENCIAR')")
	VinculoResposta desligar(@PathVariable UUID id, @Valid @RequestBody NovoDesligamentoColaborador novo) {
		return servico.desligar(id, novo);
	}

}
