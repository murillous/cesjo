package br.org.cesjo.sgi.estatisticas.interno;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.relatorios.FormatoExportacao;

/**
 * Relatório de transparência de RF016 (ADR 0210). O período vai na URL como {@code inicio} e
 * {@code fim}, datas ISO inclusivas; o formato da exportação como na base, {@code pdf} ou {@code xlsx}
 * (ADR 0042). Fica num controlador próprio, separado dos relatórios estatísticos.
 */
@RestController
@RequestMapping("/api/estatisticas/relatorios/transparencia")
class TransparenciaControlador {

	private final ServicoTransparencia servico;

	TransparenciaControlador(ServicoTransparencia servico) {
		this.servico = servico;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('RELATORIOS_LER')")
	ResponseEntity<RelatorioTransparencia> gerar(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servico.gerar(new Periodo(inicio, fim)));
	}

	@GetMapping("/exportacao")
	@PreAuthorize("hasAuthority('RELATORIOS_LER')")
	ResponseEntity<byte[]> exportar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
			@RequestParam FormatoExportacao formato) {
		return servico.exportar(new Periodo(inicio, fim), formato).comoDownload();
	}

}
