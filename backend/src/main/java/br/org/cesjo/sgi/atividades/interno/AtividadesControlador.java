package br.org.cesjo.sgi.atividades.interno;

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

import br.org.cesjo.sgi.atividades.TipoAtividade;

/** API das atividades (RF010, ADR 0150, permissões no ADR 0152). */
@RestController
@RequestMapping("/api/atividades")
class AtividadesControlador {

	private final ServicoAtividades servico;

	AtividadesControlador(ServicoAtividades servico) {
		this.servico = servico;
	}

	/**
	 * Lista paginada, da atividade mais recente para a mais antiga, sem a descrição. Filtros opcionais
	 * por tipo, período ({@code de} e {@code ate}, inclusive), profissional e usuário atendido (ADR 0155).
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('ATIVIDADES_LER')")
	Page<ResumoAtividade> listar(@RequestParam(required = false) TipoAtividade tipo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
			@RequestParam(required = false) UUID profissionalId, @RequestParam(required = false) UUID pessoaId,
			@PageableDefault(size = 20) @SortDefault.SortDefaults({
					@SortDefault(sort = "data", direction = Sort.Direction.DESC),
					@SortDefault(sort = "criadoEm", direction = Sort.Direction.DESC) }) Pageable pagina) {
		return servico.listar(new FiltroAtividades(tipo, de, ate, profissionalId, pessoaId), pagina);
	}

	/** Profissionais e usuários que aparecem em alguma atividade, para os filtros da listagem. */
	@GetMapping("/filtros")
	@PreAuthorize("hasAuthority('ATIVIDADES_LER')")
	OpcoesFiltro filtros() {
		return servico.opcoesFiltro();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('ATIVIDADES_LER')")
	AtividadeResposta ler(@PathVariable UUID id) {
		return servico.ler(id);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('ATIVIDADES_REGISTRAR')")
	ResponseEntity<AtividadeResposta> registrar(@Valid @RequestBody DadosAtividade dados) {
		AtividadeResposta atividade = servico.registrar(dados);
		return ResponseEntity.created(URI.create("/api/atividades/" + atividade.id())).body(atividade);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('ATIVIDADES_REGISTRAR')")
	AtividadeResposta corrigir(@PathVariable UUID id, @Valid @RequestBody DadosAtividade dados) {
		return servico.corrigir(id, dados);
	}

	/** Usuários ativos e profissionais ativos para o formulário de registro. */
	@GetMapping("/opcoes")
	@PreAuthorize("hasAuthority('ATIVIDADES_REGISTRAR')")
	OpcoesAtividade opcoes() {
		return servico.opcoes();
	}

}
