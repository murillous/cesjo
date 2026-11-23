package br.org.cesjo.sgi.estatisticas.interno;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.relatorios.ArquivoExportado;
import br.org.cesjo.sgi.relatorios.Exportacao;
import br.org.cesjo.sgi.relatorios.FiltroAplicado;
import br.org.cesjo.sgi.relatorios.FormatoExportacao;
import br.org.cesjo.sgi.relatorios.TabelaExportacao;

/**
 * Relatório de transparência de RF016 (ADR 0210): calcula a lista de {@link IndicadoresDaTransparencia}
 * para um período qualquer e exporta pela base do módulo Relatórios, que registra a exportação na
 * auditoria (ADR 0043).
 */
@Service
class ServicoTransparencia {

	/** Entidade da exportação no log de auditoria. */
	static final String ENTIDADE = "relatorio_transparencia";

	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final IndicadoresDaTransparencia indicadores;

	private final Exportacao exportacao;

	private final Clock relogio = Clock.systemUTC();

	ServicoTransparencia(IndicadoresDaTransparencia indicadores, Exportacao exportacao) {
		this.indicadores = indicadores;
		this.exportacao = exportacao;
	}

	/** Relatório do período; o fim não pode passar de hoje, e um período até hoje sai parcial. */
	RelatorioTransparencia gerar(Periodo periodo) {
		if (periodo.fim().isAfter(LocalDate.now(relogio.withZone(Fuso.CENTRO_DIA)))) {
			throw new RegraNegocioException("O relatório de transparência só pode ir até hoje.");
		}
		return new RelatorioTransparencia(titulo(periodo), periodo.inicio(), periodo.fim(), relogio.instant(),
				indicadores.calcular(periodo));
	}

	ArquivoExportado exportar(Periodo periodo, FormatoExportacao formato) {
		RelatorioTransparencia relatorio = gerar(periodo);
		List<List<Object>> linhas = relatorio.linhas()
			.stream()
			.map(linha -> List.<Object>of(linha.grupo(), linha.indicador(), celula(linha)))
			.toList();
		FiltroAplicado filtro = new FiltroAplicado("Período",
				DATA.format(relatorio.inicio()) + " a " + DATA.format(relatorio.fim()));
		TabelaExportacao tabela = new TabelaExportacao(relatorio.titulo(), List.of("Grupo", "Indicador", "Valor"),
				linhas, List.of(filtro));
		return exportacao.exportar(ENTIDADE, tabela, formato);
	}

	/** Ano inteiro vira "de 2025"; outro período, "de 01/01/2025 a 30/06/2025". */
	static String titulo(Periodo periodo) {
		LocalDate inicio = periodo.inicio();
		LocalDate fim = periodo.fim();
		if (inicio.getDayOfYear() == 1 && fim.getYear() == inicio.getYear()
				&& fim.equals(inicio.withDayOfYear(inicio.lengthOfYear()))) {
			return "Relatório de transparência de " + inicio.getYear();
		}
		return "Relatório de transparência de " + DATA.format(inicio) + " a " + DATA.format(fim);
	}

	/** Quantidade como número; reais como texto "R$ 1.234,50", para o PDF e a planilha mostrarem a unidade. */
	private static Object celula(LinhaTransparencia linha) {
		return switch (linha.unidade()) {
			case QUANTIDADE -> linha.valor().longValueExact();
			case REAIS -> reais(linha.valor());
		};
	}

	static String reais(BigDecimal valor) {
		DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
		return "R$ " + formato.format(valor);
	}

}
