package br.org.cesjo.sgi.listaespera.interno;

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

/** API das inscrições na lista de espera (RF001, RF002, ADR 0037). */
@RestController
@RequestMapping("/api/lista-espera/inscricoes")
class ListaEsperaControlador {

	private final ServicoListaEspera servico;

	ListaEsperaControlador(ServicoListaEspera servico) {
		this.servico = servico;
	}

	@PostMapping
	@PreAuthorize("hasAuthority('LISTA_ESPERA_CADASTRAR')")
	ResponseEntity<InscricaoResposta> inscrever(@Valid @RequestBody NovaInscricao nova) {
		InscricaoResposta inscricao = servico.inscrever(nova);
		return ResponseEntity.created(URI.create("/api/lista-espera/inscricoes/" + inscricao.id())).body(inscricao);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('LISTA_ESPERA_LER')")
	InscricaoResposta obter(@PathVariable UUID id) {
		return servico.obter(id);
	}

}
