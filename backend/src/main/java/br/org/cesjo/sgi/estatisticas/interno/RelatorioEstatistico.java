package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Relatório estatístico calculado, para a tela e para a exportação (RF016, ADR 0200). */
record RelatorioEstatistico(String titulo, LocalDate inicio, LocalDate fim, Instant geradoEm,
		List<LinhaRelatorio> linhas) {
}
