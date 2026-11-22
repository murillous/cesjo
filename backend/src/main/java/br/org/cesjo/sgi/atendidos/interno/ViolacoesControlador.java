package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.atendidos.TipoViolacao;

/** Violações de direitos dos usuários atendidos (RF009, ADR 0112 e ADR 0145). */
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

	/** Lista da ficha do usuário atendido, mais recente primeiro. */
	@GetMapping("/api/atendidos/{id}/violacoes")
	@PreAuthorize("hasAuthority('VIOLACOES_LER')")
	List<LinhaViolacao> listarDoAtendido(@PathVariable UUID id, @RequestParam(required = false) TipoViolacao tipo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
		return servico.listarDoAtendido(id, new FiltroViolacoes(tipo, de, ate));
	}

	/** Lista geral paginada, mais recente primeiro. */
	@GetMapping("/api/violacoes")
	@PreAuthorize("hasAuthority('VIOLACOES_LER')")
	Page<LinhaViolacao> listar(@RequestParam(required = false) TipoViolacao tipo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
			Pageable pageable) {
		return servico.listar(new FiltroViolacoes(tipo, de, ate), pageable);
	}

	/** Violação com a descrição; a leitura é auditada. */
	@GetMapping("/api/violacoes/{id}")
	@PreAuthorize("hasAuthority('VIOLACOES_LER')")
	ViolacaoCompleta ler(@PathVariable UUID id) {
		return servico.ler(id);
	}

}
