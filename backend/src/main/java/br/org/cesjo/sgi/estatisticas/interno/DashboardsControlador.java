package br.org.cesjo.sgi.estatisticas.interno;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Dashboards de RF017 (ADR 0195 e ADR 0196). Sem cache no navegador nem no servidor: cada acesso
 * recalcula as contagens.
 */
@RestController
@RequestMapping("/api/estatisticas/dashboards")
class DashboardsControlador {

	private final ServicoDashboards servico;

	DashboardsControlador(ServicoDashboards servico) {
		this.servico = servico;
	}

	@GetMapping("/perfil")
	@PreAuthorize("hasAuthority('RELATORIOS_LER')")
	ResponseEntity<PainelPerfil> perfil() {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servico.perfil());
	}

	/** Violações, atividades e desligamentos de {@code inicio} a {@code fim}, inclusive (ADR 0198). */
	@GetMapping("/atendimento")
	@PreAuthorize("hasAuthority('RELATORIOS_LER')")
	ResponseEntity<PainelAtendimento> atendimento(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim, Authentication autenticacao) {
		// PENDENTE-CESJO(P33): o gráfico de violações só vem para quem também lê violações (ADR 0199).
		boolean comViolacoes = autenticacao.getAuthorities()
			.stream()
			.anyMatch(autoridade -> "VIOLACOES_LER".equals(autoridade.getAuthority()));
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noStore())
			.body(servico.atendimento(new Periodo(inicio, fim), comViolacoes));
	}

}
