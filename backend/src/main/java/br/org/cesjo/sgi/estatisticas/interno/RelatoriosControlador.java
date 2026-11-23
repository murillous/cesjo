package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Year;
import java.time.YearMonth;

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
 * Relatórios estatísticos de RF016 (ADR 0200 e ADR 0203). O mês vai na URL como {@code AAAA-MM}, o ano
 * como {@code AAAA} e o quadrimestre como 1, 2 ou 3; o formato da exportação como na base, {@code pdf}
 * ou {@code xlsx} (ADR 0042).
 */
@RestController
@RequestMapping("/api/estatisticas/relatorios")
class RelatoriosControlador {

	private final ServicoRelatorios servico;

	RelatoriosControlador(ServicoRelatorios servico) {
		this.servico = servico;
	}

	@GetMapping("/mensal")
	@PreAuthorize("hasAuthority('RELATORIOS_LER')")
	ResponseEntity<RelatorioEstatistico> mensal(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servico.mensal(mes));
	}

	@GetMapping("/mensal/exportacao")
	@PreAuthorize("hasAuthority('RELATORIOS_LER')")
	ResponseEntity<byte[]> exportarMensal(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
			@RequestParam FormatoExportacao formato) {
		return servico.exportarMensal(mes, formato).comoDownload();
	}

	@GetMapping("/quadrimestral")
	@PreAuthorize("hasAuthority('RELATORIOS_LER')")
	ResponseEntity<RelatorioEstatistico> quadrimestral(@RequestParam @DateTimeFormat(pattern = "yyyy") Year ano,
			@RequestParam int quadrimestre) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servico.quadrimestral(ano, quadrimestre));
	}

	@GetMapping("/quadrimestral/exportacao")
	@PreAuthorize("hasAuthority('RELATORIOS_LER')")
	ResponseEntity<byte[]> exportarQuadrimestral(@RequestParam @DateTimeFormat(pattern = "yyyy") Year ano,
			@RequestParam int quadrimestre, @RequestParam FormatoExportacao formato) {
		return servico.exportarQuadrimestral(ano, quadrimestre, formato).comoDownload();
	}

	@GetMapping("/anual")
	@PreAuthorize("hasAuthority('RELATORIOS_LER')")
	ResponseEntity<RelatorioEstatistico> anual(@RequestParam @DateTimeFormat(pattern = "yyyy") Year ano) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servico.anual(ano));
	}

	@GetMapping("/anual/exportacao")
	@PreAuthorize("hasAuthority('RELATORIOS_LER')")
	ResponseEntity<byte[]> exportarAnual(@RequestParam @DateTimeFormat(pattern = "yyyy") Year ano,
			@RequestParam FormatoExportacao formato) {
		return servico.exportarAnual(ano, formato).comoDownload();
	}

}
