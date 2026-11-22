package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.relatorios.ArquivoExportado;
import br.org.cesjo.sgi.relatorios.Exportacao;
import br.org.cesjo.sgi.relatorios.FiltroAplicado;
import br.org.cesjo.sgi.relatorios.FormatoExportacao;
import br.org.cesjo.sgi.relatorios.TabelaExportacao;

/**
 * Relatórios estatísticos de RF016 (ADR 0200). Calcula a lista de {@link IndicadoresDoRelatorio} para
 * o período e exporta pela base do módulo Relatórios, que registra a exportação na auditoria (ADR 0043).
 */
@Service
class ServicoRelatorios {

	/** Entidade da exportação no log de auditoria. */
	static final String ENTIDADE = "relatorio_estatistico";

	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final IndicadoresDoRelatorio indicadores;

	private final Exportacao exportacao;

	private final Clock relogio = Clock.systemUTC();

	ServicoRelatorios(IndicadoresDoRelatorio indicadores, Exportacao exportacao) {
		this.indicadores = indicadores;
		this.exportacao = exportacao;
	}

	/** Relatório do mês; o mês corrente sai parcial, até hoje, e mês futuro é recusado. */
	RelatorioEstatistico mensal(YearMonth mes) {
		return calcular(PeriodoDoRelatorio.mensal(exigirMesPassadoOuCorrente(mes)));
	}

	ArquivoExportado exportarMensal(YearMonth mes, FormatoExportacao formato) {
		return exportar(mensal(mes), formato);
	}

	private RelatorioEstatistico calcular(PeriodoDoRelatorio recorte) {
		Periodo periodo = recorte.periodo();
		return new RelatorioEstatistico(recorte.titulo(), periodo.inicio(), periodo.fim(), relogio.instant(),
				indicadores.calcular(periodo));
	}

	private ArquivoExportado exportar(RelatorioEstatistico relatorio, FormatoExportacao formato) {
		List<List<Object>> linhas = relatorio.linhas()
			.stream()
			.map(linha -> List.<Object>of(linha.grupo(), linha.indicador(), linha.quantidade()))
			.toList();
		FiltroAplicado periodo = new FiltroAplicado("Período",
				DATA.format(relatorio.inicio()) + " a " + DATA.format(relatorio.fim()));
		TabelaExportacao tabela = new TabelaExportacao(relatorio.titulo(), List.of("Grupo", "Indicador", "Quantidade"),
				linhas, List.of(periodo));
		return exportacao.exportar(ENTIDADE, tabela, formato);
	}

	private YearMonth exigirMesPassadoOuCorrente(YearMonth mes) {
		if (mes.isAfter(YearMonth.from(LocalDate.now(relogio.withZone(Fuso.CENTRO_DIA))))) {
			throw new RegraNegocioException("O relatório só pode ser gerado para o mês corrente ou para meses passados.");
		}
		return mes;
	}

}
