package br.org.cesjo.sgi.institucional.interno;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

/** API do cadastro de colaboradores (RF014 parcial, ADR 0045, permissões no ADR 0047). */
@RestController
@RequestMapping("/api/colaboradores")
class ColaboradoresControlador {

	private final ServicoColaboradores servico;

	ColaboradoresControlador(ServicoColaboradores servico) {
		this.servico = servico;
	}

	/** Lista paginada em ordem de nome. Sem {@code ativo}, traz ativos e inativos. */
	@GetMapping
	@PreAuthorize("hasAuthority('COLABORADORES_LER')")
	Page<ColaboradorResposta> listar(@RequestParam(required = false) Boolean ativo,
			@PageableDefault(sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
		return servico.listar(ativo, paginacao);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('COLABORADORES_LER')")
	ColaboradorResposta obter(@PathVariable UUID id) {
		return servico.obter(id);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('COLABORADORES_GERENCIAR')")
	ResponseEntity<ColaboradorResposta> criar(@Valid @RequestBody NovoColaborador novo) {
		ColaboradorResposta colaborador = servico.criar(novo);
		return ResponseEntity.created(URI.create("/api/colaboradores/" + colaborador.id())).body(colaborador);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('COLABORADORES_GERENCIAR')")
	ColaboradorResposta editar(@PathVariable UUID id, @Valid @RequestBody EdicaoColaborador edicao) {
		return servico.editar(id, edicao);
	}

}
