package br.org.cesjo.sgi.estatisticas.interno;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

}
