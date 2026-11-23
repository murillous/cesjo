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
 * Relatório de intervenção (CREAS) de RF016 (ADR 0205 e ADR 0206). O período vai na URL com
 * {@code inicio} e {@code fim} em {@code AAAA-MM-DD}, as duas inclusive; o formato da exportação como na
 * base, {@code pdf} ou {@code xlsx} (ADR 0042).
 */
@RestController
@RequestMapping("/api/estatisticas/relatorios/intervencao")
class RelatorioIntervencaoControlador {

	// PENDENTE-CESJO(P9): o relatório lista violações por nome, então pede relatórios e violações ao mesmo
	// tempo; hoje só o Coordenador tem as duas (ADR 0206).
	private static final String PODE_GERAR = "hasAuthority('RELATORIOS_LER') and hasAuthority('VIOLACOES_LER')";

	private final ServicoRelatorioIntervencao servico;

	RelatorioIntervencaoControlador(ServicoRelatorioIntervencao servico) {
		this.servico = servico;
	}

	@GetMapping
	@PreAuthorize(PODE_GERAR)
	ResponseEntity<RelatorioIntervencao> gerar(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servico.gerar(inicio, fim));
	}

	@GetMapping("/exportacao")
	@PreAuthorize(PODE_GERAR)
	ResponseEntity<byte[]> exportar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
			@RequestParam FormatoExportacao formato) {
		return servico.exportar(inicio, fim, formato).comoDownload();
	}

}
