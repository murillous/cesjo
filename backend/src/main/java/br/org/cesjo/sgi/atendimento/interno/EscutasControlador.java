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

/** API das escutas ativas e qualificadas (RF005, ADR 0100, permissões no ADR 0101). */
@RestController
@RequestMapping("/api/escutas")
class EscutasControlador {

	private final ServicoEscutas servico;

	EscutasControlador(ServicoEscutas servico) {
		this.servico = servico;
	}

	/** Escutas da pessoa, sem os textos. */
	@GetMapping
	@PreAuthorize("hasAuthority('ESCUTA_LER')")
	List<ResumoEscuta> listar(@RequestParam UUID pessoaId) {
		return servico.listar(pessoaId);
	}

	/** Escuta completa; cada leitura fica no log de auditoria. */
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('ESCUTA_LER')")
	EscutaResposta ler(@PathVariable UUID id) {
		return servico.ler(id);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('ESCUTA_REGISTRAR')")
	ResponseEntity<EscutaResposta> registrar(@Valid @RequestBody NovaEscuta nova) {
		EscutaResposta escuta = servico.registrar(nova);
		return ResponseEntity.created(URI.create("/api/escutas/" + escuta.id())).body(escuta);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('ESCUTA_REGISTRAR')")
	EscutaResposta corrigir(@PathVariable UUID id, @Valid @RequestBody DadosEscuta dados) {
		return servico.corrigir(id, dados);
	}

	@GetMapping("/profissionais")
	@PreAuthorize("hasAuthority('ESCUTA_REGISTRAR')")
	ProfissionaisDaEscuta profissionais() {
		return servico.profissionais();
	}

}
