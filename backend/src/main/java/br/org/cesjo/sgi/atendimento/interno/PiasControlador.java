package br.org.cesjo.sgi.atendimento.interno;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API do PIA (RF006, ADR 0103, permissões no ADR 0104). */
@RestController
@RequestMapping("/api/pias")
class PiasControlador {

	private final ServicoPias servico;

	PiasControlador(ServicoPias servico) {
		this.servico = servico;
	}

	/** Versões do PIA da pessoa, da mais nova para a mais antiga, sem o conteúdo. */
	@GetMapping
	@PreAuthorize("hasAuthority('PIA_LER')")
	List<ResumoPia> versoes(@RequestParam UUID pessoaId) {
		return servico.versoes(pessoaId);
	}

	/** Versão com o conteúdo; cada leitura fica no log de auditoria. */
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('PIA_LER')")
	PiaResposta ler(@PathVariable UUID id) {
		return servico.ler(id);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('PIA_EDITAR')")
	ResponseEntity<PiaResposta> elaborar(@Valid @RequestBody NovoPia novo) {
		PiaResposta pia = servico.elaborar(novo);
		return ResponseEntity.created(URI.create("/api/pias/" + pia.id())).body(pia);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('PIA_EDITAR')")
	PiaResposta editar(@PathVariable UUID id, @Valid @RequestBody ConteudoPia conteudo) {
		return servico.editar(id, conteudo);
	}

}
