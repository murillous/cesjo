package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard de violações, atividades e desligamentos num período (RF017, issue #57, ADR 0198).
 *
 * @param violacoesPorCategoria {@code null} para quem não lê violações de direitos (ADR 0199)
 * @param atendidosNoPeriodo usuários atendidos em algum dia do período, a base das taxas
 * @param taxaDesligamento todos os desligamentos do período sobre {@code atendidosNoPeriodo}
 */
record PainelAtendimento(Instant geradoEm, LocalDate inicio, LocalDate fim, List<Contagem> violacoesPorCategoria,
		List<Contagem> atividadesPorTipo, long totalAtividades, long atendidosNoPeriodo, long totalDesligamentos,
		double taxaDesligamento, List<TaxaDesligamento> desligamentosPorMotivo) {
}
