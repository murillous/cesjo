package br.org.cesjo.sgi.relatorios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class TabelaExportacaoTests {

	@Test
	void aceitaCelulaVaziaECopiaAsListas() {
		List<Object> linha = new ArrayList<>(Arrays.asList("Pessoa Fictícia", null));
		List<List<Object>> linhas = new ArrayList<>(List.of(linha));

		TabelaExportacao tabela = new TabelaExportacao("Título", List.of("Nome", "Telefone"), linhas);
		linha.set(0, "Outra");
		linhas.clear();

		assertThat(tabela.linhas()).hasSize(1);
		assertThat(tabela.linhas().get(0)).containsExactly("Pessoa Fictícia", null);
		assertThat(tabela.filtros()).isEmpty();
	}

	@Test
	void recusaLinhaComNumeroDeCelulasDiferenteDasColunas() {
		assertThatThrownBy(() -> new TabelaExportacao("Título", List.of("Nome", "Telefone"), List.of(List.of("só uma"))))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("linha 1");
	}

	@Test
	void recusaTituloEmBrancoESemColunas() {
		assertThatThrownBy(() -> new TabelaExportacao(" ", List.of("Nome"), List.of()))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new TabelaExportacao("Título", List.of(), List.of()))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void formatoVemDoParametroSemDiferenciarMaiusculas() {
		assertThat(FormatoExportacao.doParametro("pdf")).isEqualTo(FormatoExportacao.PDF);
		assertThat(FormatoExportacao.doParametro(" XLSX ")).isEqualTo(FormatoExportacao.XLSX);
		assertThatThrownBy(() -> FormatoExportacao.doParametro("csv")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FormatoExportacao.doParametro(null)).isInstanceOf(IllegalArgumentException.class);
	}

}
