package br.org.cesjo.sgi.atendidos.interno;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inserção de candidato da fila no serviço (RF007, ADR 0080). */
@RestController
@RequestMapping("/api/atendidos")
class InsercaoControlador {

	private final ServicoInsercao servico;

	InsercaoControlador(ServicoInsercao servico) {
		this.servico = servico;
	}

	/** Dados do candidato para a tela de inserção: nome, situação na fila e responsáveis. */
	@GetMapping("/candidatos/{inscricaoId}")
	@PreAuthorize("hasAuthority('ATENDIDOS_INSERIR')")
	CandidatoInsercao candidato(@PathVariable UUID inscricaoId) {
		return servico.candidato(inscricaoId);
	}

	@PostMapping("/insercoes")
	@PreAuthorize("hasAuthority('ATENDIDOS_INSERIR')")
	ResponseEntity<InsercaoResposta> inserir(@Valid @RequestBody NovaInsercao nova) {
		InsercaoResposta insercao = servico.inserir(nova);
		return ResponseEntity.created(URI.create("/api/atendidos/" + insercao.id())).body(insercao);
	}

}
