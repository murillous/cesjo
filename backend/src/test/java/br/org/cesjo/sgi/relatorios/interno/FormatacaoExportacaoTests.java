package br.org.cesjo.sgi.relatorios.interno;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.org.cesjo.sgi.relatorios.FormatoExportacao;

class FormatacaoExportacaoTests {

	@Test
	void celulasNoPadraoBrasileiro() {
		assertThat(FormatacaoCelula.texto(null)).isEmpty();
		assertThat(FormatacaoCelula.texto(true)).isEqualTo("Sim");
		assertThat(FormatacaoCelula.texto(false)).isEqualTo("Não");
		assertThat(FormatacaoCelula.texto(LocalDate.of(2026, 1, 5))).isEqualTo("05/01/2026");
		assertThat(FormatacaoCelula.texto(LocalDateTime.of(2026, 1, 5, 9, 7))).isEqualTo("05/01/2026 09:07");
		// 12h UTC são 9h em Brasília.
		assertThat(FormatacaoCelula.texto(Instant.parse("2026-01-05T12:00:00Z"))).isEqualTo("05/01/2026 09:00");
		assertThat(FormatacaoCelula.texto(2026)).isEqualTo("2026");
		assertThat(FormatacaoCelula.texto(new BigDecimal("1234.567"))).isEqualTo("1234,57");
		assertThat(FormatacaoCelula.texto(0.5)).isEqualTo("0,5");
	}

	@Test
	void nomeDoArquivoSemAcentoComDataDeBrasilia() {
		// 01h UTC do dia 22 ainda é dia 21 em Brasília.
		Instant geradoEm = Instant.parse("2026-11-22T01:00:00Z");

		assertThat(ServicoExportacao.nomeArquivo("Lista de espera — Situação: Aguardando", geradoEm,
				FormatoExportacao.PDF))
			.isEqualTo("lista-de-espera-situacao-aguardando-2026-11-21.pdf");
		assertThat(ServicoExportacao.nomeArquivo("???", geradoEm, FormatoExportacao.XLSX))
			.isEqualTo("exportacao-2026-11-21.xlsx");
		assertThat(ServicoExportacao.nomeArquivo("a".repeat(200), geradoEm, FormatoExportacao.PDF))
			.hasSize(ServicoExportacao.TAMANHO_MAXIMO_NOME + "-2026-11-21.pdf".length());
	}

}
