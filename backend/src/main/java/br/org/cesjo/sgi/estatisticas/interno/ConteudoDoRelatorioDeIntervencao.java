package br.org.cesjo.sgi.estatisticas.interno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.atendidos.CasoDeViolacao;
import br.org.cesjo.sgi.atendidos.CasosDeViolacao;
import br.org.cesjo.sgi.atendidos.TipoViolacao;
import br.org.cesjo.sgi.atividades.AtendimentosIndividuais;
import br.org.cesjo.sgi.atividades.Comunicacoes;
import br.org.cesjo.sgi.atividades.DirecaoComunicacao;
import br.org.cesjo.sgi.atividades.TipoComunicacao;

/**
 * O conteúdo do relatório de intervenção (CREAS) de RF016 (ADR 0205): os casos de violação de direitos
 * identificados no período e o que o serviço fez em cada um, mais um resumo do período. Mudar o
 * relatório é mudar esta classe. Cada dado vem do módulo dono pela API Java pública dele.
 */
@Component
class ConteudoDoRelatorioDeIntervencao {

	static final String CASOS = "Casos";

	static final String VIOLACOES = "Violações por categoria";

	static final String INTERVENCOES = "Intervenções";

	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final CasosDeViolacao casosDeViolacao;

	private final AtendimentosIndividuais atendimentos;

	private final Comunicacoes comunicacoes;

	ConteudoDoRelatorioDeIntervencao(CasosDeViolacao casosDeViolacao, AtendimentosIndividuais atendimentos,
			Comunicacoes comunicacoes) {
		this.casosDeViolacao = casosDeViolacao;
		this.atendimentos = atendimentos;
		this.comunicacoes = comunicacoes;
	}

	// PENDENTE-CESJO(P9): conteúdo provisório do relatório de intervenção, sem modelo da equipe (ADR 0205).
	// Casos pela data de identificação da violação; intervenções são os atendimentos individuais do usuário
	// no período, a situação no serviço no fim do período e, no resumo, os encaminhamentos do período, que
	// não são ligados ao usuário (P30). A descrição da violação não entra.
	Conteudo calcular(Periodo periodo) {
		List<CasoDeViolacao> casos = casosDeViolacao.casosNoPeriodo(periodo.inicio(), periodo.fim());
		Map<UUID, AtendimentosIndividuais.Resumo> atendidos = atendimentos
			.resumirPorPessoa(casos.stream().map(CasoDeViolacao::pessoaId).toList(), periodo.inicio(), periodo.fim());

		List<RelatorioIntervencao.Caso> linhas = new ArrayList<>(casos.size());
		Map<TipoViolacao, Long> porCategoria = new EnumMap<>(TipoViolacao.class);
		long violacoes = 0;
		long totalAtendimentos = 0;
		long semAtendimento = 0;
		long desligados = 0;
		for (CasoDeViolacao caso : casos) {
			AtendimentosIndividuais.Resumo resumo = atendidos.get(caso.pessoaId());
			for (CasoDeViolacao.Violacao violacao : caso.violacoes()) {
				porCategoria.merge(violacao.tipo(), 1L, Long::sum);
			}
			violacoes += caso.violacoes().size();
			totalAtendimentos += resumo == null ? 0 : resumo.quantidade();
			semAtendimento += resumo == null ? 1 : 0;
			desligados += caso.dataDesligamento() == null ? 0 : 1;
			linhas.add(new RelatorioIntervencao.Caso(caso.nomeCompleto(),
					caso.violacoes().stream().map(ConteudoDoRelatorioDeIntervencao::violacao).toList(),
					resumo == null ? 0 : resumo.quantidade(), resumo == null ? null : resumo.ultimo(), situacao(caso)));
		}

		List<LinhaRelatorio> resumo = new ArrayList<>();
		resumo.add(new LinhaRelatorio(CASOS, "Usuários com violação identificada", casos.size()));
		resumo.add(new LinhaRelatorio(CASOS, "Violações identificadas", violacoes));
		for (TipoViolacao tipo : TipoViolacao.values()) {
			resumo.add(new LinhaRelatorio(VIOLACOES, tipo.rotulo(), porCategoria.getOrDefault(tipo, 0L)));
		}
		resumo.add(new LinhaRelatorio(INTERVENCOES, "Atendimentos individuais aos usuários com violação",
				totalAtendimentos));
		resumo.add(new LinhaRelatorio(INTERVENCOES, "Usuários com violação sem atendimento no período", semAtendimento));
		Map<DirecaoComunicacao, Long> encaminhamentos = comunicacoes.contarPorDirecao(TipoComunicacao.ENCAMINHAMENTO,
				periodo.inicio(), periodo.fim());
		for (DirecaoComunicacao direcao : DirecaoComunicacao.values()) {
			resumo.add(new LinhaRelatorio(INTERVENCOES,
					RotulosComunicacao.de(TipoComunicacao.ENCAMINHAMENTO, direcao) + " (todos os usuários)",
					encaminhamentos.getOrDefault(direcao, 0L)));
		}
		resumo.add(new LinhaRelatorio(INTERVENCOES, "Usuários com violação desligados até o fim do período", desligados));
		return new Conteudo(List.copyOf(resumo), List.copyOf(linhas));
	}

	private static RelatorioIntervencao.Violacao violacao(CasoDeViolacao.Violacao violacao) {
		return new RelatorioIntervencao.Violacao(violacao.tipo().rotulo(),
				violacao.ambito() == null ? null : violacao.ambito().rotulo(), violacao.dataIdentificacao());
	}

	private static String situacao(CasoDeViolacao caso) {
		if (!caso.inserido()) {
			return "Sem inserção no serviço";
		}
		LocalDate desligamento = caso.dataDesligamento();
		if (desligamento == null) {
			return "Ativo no serviço";
		}
		return "Desligado em " + DATA.format(desligamento) + ": " + caso.motivoDesligamento().rotulo();
	}

	/** O que o relatório mostra, antes do título e das datas. */
	record Conteudo(List<LinhaRelatorio> resumo, List<RelatorioIntervencao.Caso> casos) {
	}

}
