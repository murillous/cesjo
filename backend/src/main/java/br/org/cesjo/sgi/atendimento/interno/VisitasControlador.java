package br.org.cesjo.sgi.atendimento.interno;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.institucional.ResumoColaborador;
import br.org.cesjo.sgi.listaespera.CandidatoAtivo;

/** API das visitas domiciliares (RF004, ADR 0070, permissões no ADR 0071). */
@RestController
@RequestMapping("/api/visitas")
class VisitasControlador {

	private final ServicoVisitas servico;

	VisitasControlador(ServicoVisitas servico) {
		this.servico = servico;
	}

	/** Agenda de um período de até 62 dias, datas inclusive. */
	@GetMapping
	@PreAuthorize("hasAuthority('VISITAS_LER')")
	List<VisitaResposta> agenda(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
		return servico.agenda(de, ate);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('VISITAS_LER')")
	VisitaResposta obter(@PathVariable UUID id) {
		return servico.obter(id);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('VISITAS_AGENDAR')")
	ResponseEntity<VisitaResposta> agendar(@Valid @RequestBody NovaVisita nova) {
		VisitaResposta visita = servico.agendar(nova);
		return ResponseEntity.created(URI.create("/api/visitas/" + visita.id())).body(visita);
	}

	/** Candidatos da lista de espera que podem receber visita. */
	@GetMapping("/candidatos")
	@PreAuthorize("hasAuthority('VISITAS_AGENDAR')")
	List<CandidatoAtivo> candidatos() {
		return servico.candidatos();
	}

	/** Colaboradores ativos que podem ser escolhidos como profissionais. */
	@GetMapping("/profissionais")
	@PreAuthorize("hasAuthority('VISITAS_AGENDAR')")
	List<ResumoColaborador> profissionais() {
		return servico.profissionais();
	}

}
