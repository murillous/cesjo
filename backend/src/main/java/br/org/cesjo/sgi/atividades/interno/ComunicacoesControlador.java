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

import br.org.cesjo.sgi.atividades.TipoComunicacao;

/** API dos contatos e comunicações (RF011, ADR 0160, permissões no ADR 0162). */
@RestController
@RequestMapping("/api/comunicacoes")
class ComunicacoesControlador {

	private final ServicoComunicacoes servico;

	ComunicacoesControlador(ServicoComunicacoes servico) {
		this.servico = servico;
	}

	/** Lista paginada, da mais recente para a mais antiga, sem a descrição. Filtros opcionais. */
	@GetMapping
	@PreAuthorize("hasAuthority('COMUNICACOES_LER')")
	Page<ResumoComunicacao> listar(@RequestParam(required = false) TipoComunicacao tipo,
			@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
			@PageableDefault(size = 20) @SortDefault.SortDefaults({
					@SortDefault(sort = "data", direction = Sort.Direction.DESC),
					@SortDefault(sort = "criadoEm", direction = Sort.Direction.DESC) }) Pageable pagina) {
		return servico.listar(new FiltroComunicacoes(tipo, inicio, fim), pagina);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('COMUNICACOES_LER')")
	ComunicacaoResposta ler(@PathVariable UUID id) {
		return servico.ler(id);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('COMUNICACOES_REGISTRAR')")
	ResponseEntity<ComunicacaoResposta> registrar(@Valid @RequestBody DadosComunicacao dados) {
		ComunicacaoResposta comunicacao = servico.registrar(dados);
		return ResponseEntity.created(URI.create("/api/comunicacoes/" + comunicacao.id())).body(comunicacao);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('COMUNICACOES_REGISTRAR')")
	ComunicacaoResposta corrigir(@PathVariable UUID id, @Valid @RequestBody DadosComunicacao dados) {
		return servico.corrigir(id, dados);
	}

}
