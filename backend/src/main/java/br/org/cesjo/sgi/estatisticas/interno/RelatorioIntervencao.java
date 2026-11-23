package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Relatório de intervenção (CREAS) calculado, para a tela e para a exportação (RF016, ADR 0205).
 *
 * @param resumo totais do período, na ordem de {@link ConteudoDoRelatorioDeIntervencao}
 * @param casos um por usuário com violação identificada no período, em ordem de nome
 */
record RelatorioIntervencao(String titulo, LocalDate inicio, LocalDate fim, Instant geradoEm,
		List<LinhaRelatorio> resumo, List<Caso> casos) {

	/**
	 * Um usuário atendido com violação identificada no período e as intervenções do serviço.
	 *
	 * @param usuario nome do usuário atendido
	 * @param violacoes violações identificadas no período, da mais antiga para a mais recente
	 * @param atendimentos atendimentos individuais do usuário no período
	 * @param ultimoAtendimento data do atendimento individual mais recente no período, {@code null} se não houve
	 * @param situacao situação no serviço no fim do período, já em texto
	 */
	record Caso(String usuario, List<Violacao> violacoes, long atendimentos, LocalDate ultimoAtendimento,
			String situacao) {
	}

	/**
	 * Uma violação do caso, sem a descrição.
	 *
	 * @param ambito "Intrafamiliar" ou "Extrafamiliar", {@code null} nas categorias sem âmbito
	 */
	record Violacao(String categoria, String ambito, LocalDate identificadaEm) {
	}

}
