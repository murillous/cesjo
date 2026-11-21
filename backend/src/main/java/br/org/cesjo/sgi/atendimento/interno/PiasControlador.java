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

/** API do PIA (RF006, ADR 0103 e ADR 0106, permissões nos ADR 0104 e ADR 0107). */
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

	@PostMapping("/{id}/apresentar-a-familia")
	@PreAuthorize("hasAuthority('PIA_EDITAR')")
	PiaResposta apresentarAFamilia(@PathVariable UUID id, @Valid @RequestBody DataDoPasso passo) {
		return servico.avancar(id, StatusPia.APRESENTADO_FAMILIA, passo.data());
	}

	@PostMapping("/{id}/apresentar-a-equipe")
	@PreAuthorize("hasAuthority('PIA_EDITAR')")
	PiaResposta apresentarAEquipe(@PathVariable UUID id, @Valid @RequestBody DataDoPasso passo) {
		return servico.avancar(id, StatusPia.APRESENTADO_EQUIPE, passo.data());
	}

	/** Só o Coordenador aprova (seção 4.1). */
	@PostMapping("/{id}/aprovar")
	@PreAuthorize("hasAuthority('PIA_APROVAR')")
	PiaResposta aprovar(@PathVariable UUID id, @Valid @RequestBody DataDoPasso passo) {
		return servico.avancar(id, StatusPia.APROVADO, passo.data());
	}

	/** Versão nova em elaboração, com o conteúdo da atual (201 com {@code Location}). */
	@PostMapping("/{id}/nova-versao")
	@PreAuthorize("hasAuthority('PIA_EDITAR')")
	ResponseEntity<PiaResposta> novaVersao(@PathVariable UUID id) {
		PiaResposta nova = servico.novaVersao(id);
		return ResponseEntity.created(URI.create("/api/pias/" + nova.id())).body(nova);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('PIA_EDITAR')")
	PiaResposta editar(@PathVariable UUID id, @Valid @RequestBody ConteudoPia conteudo) {
		return servico.editar(id, conteudo);
	}

}
