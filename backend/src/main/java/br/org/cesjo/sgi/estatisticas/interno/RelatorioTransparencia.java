package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Relatório de transparência calculado, para a tela e para a exportação (RF016, ADR 0210). */
record RelatorioTransparencia(String titulo, LocalDate inicio, LocalDate fim, Instant geradoEm,
		List<LinhaTransparencia> linhas) {
}
