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

import br.org.cesjo.sgi.institucional.TipoEventoInstitucional;

/** API das reuniões e eventos institucionais (RF012, ADR 0175, permissões no ADR 0176). */
@RestController
@RequestMapping("/api/eventos-institucionais")
class EventosControlador {

	private final ServicoEventos servico;

	EventosControlador(ServicoEventos servico) {
		this.servico = servico;
	}

	/** Lista paginada do mais recente ao mais antigo, com filtros opcionais de tipo, período e parceiro. */
	@GetMapping
	@PreAuthorize("hasAuthority('EVENTOS_LER')")
	Page<EventoResposta> listar(@RequestParam(required = false) TipoEventoInstitucional tipo,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate de,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate ate,
			@RequestParam(required = false) UUID parceiroId,
			@PageableDefault @SortDefault.SortDefaults({ @SortDefault(sort = "data", direction = Sort.Direction.DESC),
					@SortDefault(sort = "criadoEm", direction = Sort.Direction.DESC) }) Pageable paginacao) {
		return servico.listar(tipo, de, ate, parceiroId, paginacao);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('EVENTOS_LER')")
	EventoResposta obter(@PathVariable UUID id) {
		return servico.obter(id);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('EVENTOS_REGISTRAR')")
	ResponseEntity<EventoResposta> registrar(@Valid @RequestBody DadosEvento dados) {
		EventoResposta evento = servico.registrar(dados);
		return ResponseEntity.created(URI.create("/api/eventos-institucionais/" + evento.id())).body(evento);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('EVENTOS_REGISTRAR')")
	EventoResposta editar(@PathVariable UUID id, @Valid @RequestBody DadosEvento dados) {
		return servico.editar(id, dados);
	}

}
