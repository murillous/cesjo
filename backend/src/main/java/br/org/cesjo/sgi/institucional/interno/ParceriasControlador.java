package br.org.cesjo.sgi.institucional.interno;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API dos parceiros e das ações de articulação (RF013, ADR 0172, permissões no ADR 0173). */
@RestController
@RequestMapping("/api/parceiros")
class ParceriasControlador {

	private final ServicoParcerias servico;

	ParceriasControlador(ServicoParcerias servico) {
		this.servico = servico;
	}

	/** Lista paginada em ordem de nome, com filtros opcionais de situação e nome. */
	@GetMapping
	@PreAuthorize("hasAuthority('PARCERIAS_LER')")
	Page<ParceiroResposta> listar(@RequestParam(required = false) Boolean ativo,
			@RequestParam(required = false) String nome,
			@PageableDefault(sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
		return servico.listar(ativo, nome, paginacao);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('PARCERIAS_LER')")
	ParceiroResposta obter(@PathVariable UUID id) {
		return servico.obter(id);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('PARCERIAS_GERENCIAR')")
	ResponseEntity<ParceiroResposta> criar(@Valid @RequestBody DadosParceiro dados) {
		ParceiroResposta parceiro = servico.criar(dados);
		return ResponseEntity.created(URI.create("/api/parceiros/" + parceiro.id())).body(parceiro);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('PARCERIAS_GERENCIAR')")
	ParceiroResposta editar(@PathVariable UUID id, @Valid @RequestBody DadosParceiro dados) {
		return servico.editar(id, dados);
	}

	/** Ações de articulação da mais recente à mais antiga. */
	@GetMapping("/{id}/articulacoes")
	@PreAuthorize("hasAuthority('PARCERIAS_LER')")
	List<ArticulacaoResposta> listarArticulacoes(@PathVariable UUID id) {
		return servico.listarArticulacoes(id);
	}

	@PostMapping("/{id}/articulacoes")
	@PreAuthorize("hasAuthority('PARCERIAS_GERENCIAR')")
	@ResponseStatus(HttpStatus.CREATED)
	ArticulacaoResposta registrarArticulacao(@PathVariable UUID id, @Valid @RequestBody NovaArticulacao nova) {
		return servico.registrarArticulacao(id, nova);
	}

}
