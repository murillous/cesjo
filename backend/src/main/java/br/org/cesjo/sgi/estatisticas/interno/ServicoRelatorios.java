package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
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

	/** Relatório do quadrimestre (1 a 3); o quadrimestre corrente sai parcial e um futuro é recusado. */
	RelatorioEstatistico quadrimestral(Year ano, int quadrimestre) {
		if (quadrimestre < 1 || quadrimestre > PeriodoDoRelatorio.QUADRIMESTRES_NO_ANO) {
			throw new RegraNegocioException("O quadrimestre deve ser 1, 2 ou 3.");
		}
		return calcular(exigirInicioPassadoOuHoje(PeriodoDoRelatorio.quadrimestral(ano, quadrimestre)));
	}

	ArquivoExportado exportarQuadrimestral(Year ano, int quadrimestre, FormatoExportacao formato) {
		return exportar(quadrimestral(ano, quadrimestre), formato);
	}

	/** Relatório do ano; o ano corrente sai parcial e um ano futuro é recusado. */
	RelatorioEstatistico anual(Year ano) {
		return calcular(exigirInicioPassadoOuHoje(PeriodoDoRelatorio.anual(ano)));
	}

	ArquivoExportado exportarAnual(Year ano, FormatoExportacao formato) {
		return exportar(anual(ano), formato);
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

	private PeriodoDoRelatorio exigirInicioPassadoOuHoje(PeriodoDoRelatorio recorte) {
		if (recorte.periodo().inicio().isAfter(hoje())) {
			throw new RegraNegocioException(
					"O relatório só pode ser gerado para o período corrente ou para períodos passados.");
		}
		return recorte;
	}

	private LocalDate hoje() {
		return LocalDate.now(relogio.withZone(Fuso.CENTRO_DIA));
	}

	private YearMonth exigirMesPassadoOuCorrente(YearMonth mes) {
		if (mes.isAfter(YearMonth.from(hoje()))) {
			throw new RegraNegocioException("O relatório só pode ser gerado para o mês corrente ou para meses passados.");
		}
		return mes;
	}

}
