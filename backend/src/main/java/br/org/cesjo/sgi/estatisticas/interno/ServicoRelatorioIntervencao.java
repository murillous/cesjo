package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.relatorios.ArquivoExportado;
import br.org.cesjo.sgi.relatorios.Exportacao;
import br.org.cesjo.sgi.relatorios.FiltroAplicado;
import br.org.cesjo.sgi.relatorios.FormatoExportacao;
import br.org.cesjo.sgi.relatorios.TabelaExportacao;

/**
 * Relatório de intervenção (CREAS) de RF016 (ADR 0205 e ADR 0206). O relatório traz o nome de quem
 * sofreu violação de direitos, então gerar na tela registra leitura sensível e exportar registra a
 * exportação, as duas com a entidade {@value #ENTIDADE}.
 */
@Service
class ServicoRelatorioIntervencao {

	/** Entidade do relatório no log de auditoria. */
	static final String ENTIDADE = "relatorio_intervencao";

	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private static final List<String> COLUNAS = List.of("Usuário atendido", "Violações identificadas",
			"Atendimentos no período", "Último atendimento", "Situação no fim do período");

	private final ConteudoDoRelatorioDeIntervencao conteudo;

	private final Exportacao exportacao;

	private final Auditoria auditoria;

	private final Clock relogio = Clock.systemUTC();

	ServicoRelatorioIntervencao(ConteudoDoRelatorioDeIntervencao conteudo, Exportacao exportacao, Auditoria auditoria) {
		this.conteudo = conteudo;
		this.exportacao = exportacao;
		this.auditoria = auditoria;
	}

	/** Relatório para a tela. Registra a leitura sensível com o período como identificador. */
	RelatorioIntervencao gerar(LocalDate inicio, LocalDate fim) {
		RelatorioIntervencao relatorio = calcular(inicio, fim);
		auditoria.registrar(EventoAuditoria.leituraSensivel(ENTIDADE, inicio + "/" + fim));
		return relatorio;
	}

	/** Arquivo do relatório. A base de exportação registra a exportação (ADR 0043). */
	ArquivoExportado exportar(LocalDate inicio, LocalDate fim, FormatoExportacao formato) {
		RelatorioIntervencao relatorio = calcular(inicio, fim);
		List<List<Object>> linhas = relatorio.casos()
			.stream()
			.map(caso -> {
				List<Object> linha = new ArrayList<>(COLUNAS.size());
				linha.add(caso.usuario());
				linha.add(caso.violacoes()
					.stream()
					.map(ServicoRelatorioIntervencao::violacao)
					.collect(Collectors.joining("; ")));
				linha.add(caso.atendimentos());
				linha.add(caso.ultimoAtendimento());
				linha.add(caso.situacao());
				return linha;
			})
			.toList();
		List<FiltroAplicado> cabecalho = new ArrayList<>();
		cabecalho.add(new FiltroAplicado("Período", DATA.format(inicio) + " a " + DATA.format(fim)));
		// O resumo vai no cabeçalho, menos as categorias, que já aparecem nas linhas.
		relatorio.resumo()
			.stream()
			.filter(linha -> !linha.grupo().equals(ConteudoDoRelatorioDeIntervencao.VIOLACOES))
			.forEach(linha -> cabecalho.add(new FiltroAplicado(linha.indicador(), String.valueOf(linha.quantidade()))));
		return exportacao.exportar(ENTIDADE, new TabelaExportacao(relatorio.titulo(), COLUNAS, linhas, cabecalho),
				formato);
	}

	private RelatorioIntervencao calcular(LocalDate inicio, LocalDate fim) {
		Periodo periodo = new Periodo(inicio, fim);
		if (fim.isAfter(LocalDate.now(relogio.withZone(Fuso.CENTRO_DIA)))) {
			throw new RegraNegocioException("O relatório de intervenção só pode ir até hoje.");
		}
		ConteudoDoRelatorioDeIntervencao.Conteudo calculado = conteudo.calcular(periodo);
		String titulo = "Relatório de intervenção de " + DATA.format(inicio) + " a " + DATA.format(fim);
		return new RelatorioIntervencao(titulo, inicio, fim, relogio.instant(), calculado.resumo(), calculado.casos());
	}

	private static String violacao(RelatorioIntervencao.Violacao violacao) {
		String ambito = violacao.ambito() == null ? "" : ", " + violacao.ambito().toLowerCase(Locale.ROOT);
		return violacao.categoria() + ambito + " (" + DATA.format(violacao.identificadaEm()) + ")";
	}

}
