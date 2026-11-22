package br.org.cesjo.sgi.institucional.interno;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
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

import br.org.cesjo.sgi.institucional.DirecaoDoacao;
import br.org.cesjo.sgi.institucional.TipoDoacao;

/** API das doações (RF015, ADR 0178, permissões no ADR 0179). */
@RestController
@RequestMapping("/api/doacoes")
class DoacoesControlador {

	private final ServicoDoacoes servico;

	DoacoesControlador(ServicoDoacoes servico) {
		this.servico = servico;
	}

	/** Lista paginada da mais recente à mais antiga, com filtros opcionais de período, direção e tipo. */
	@GetMapping
	@PreAuthorize("hasAuthority('DOACOES_LER')")
	Page<DoacaoResposta> listar(@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate de,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate ate,
			@RequestParam(required = false) DirecaoDoacao direcao, @RequestParam(required = false) TipoDoacao tipo,
			@PageableDefault @SortDefault.SortDefaults({ @SortDefault(sort = "data", direction = Sort.Direction.DESC),
					@SortDefault(sort = "criadoEm", direction = Sort.Direction.DESC) }) Pageable paginacao) {
		return servico.listar(de, ate, direcao, tipo, paginacao);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('DOACOES_LER')")
	DoacaoResposta obter(@PathVariable UUID id) {
		return servico.obter(id);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('DOACOES_REGISTRAR')")
	ResponseEntity<DoacaoResposta> registrar(@Valid @RequestBody DadosDoacao dados) {
		DoacaoResposta doacao = servico.registrar(dados);
		return ResponseEntity.created(URI.create("/api/doacoes/" + doacao.id())).body(doacao);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('DOACOES_REGISTRAR')")
	DoacaoResposta editar(@PathVariable UUID id, @Valid @RequestBody DadosDoacao dados) {
		return servico.editar(id, dados);
	}

}
