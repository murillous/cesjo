package br.org.cesjo.sgi.atividades;

import java.time.LocalDate;
import java.util.Map;

/**
 * API pública do módulo Atividades para os outros módulos (ADR 0151). Os dashboards e os relatórios
 * (issues #57 e #58) contam as atividades realizadas por período e por tipo sem conhecer as tabelas do
 * módulo.
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil. Só conta, sem expor descrição nem
 * participantes, então não audita.
 */
public interface Atividades {

	/** Nome da entidade atividade no log de auditoria. */
	String ENTIDADE = "atividade";

	/**
	 * Atividades com data entre {@code inicio} e {@code fim}, as duas inclusive, contadas por tipo. Todo
	 * tipo aparece no mapa, com zero quando não houve atividade.
	 *
	 * @throws IllegalArgumentException quando {@code fim} é anterior a {@code inicio}
	 */
	Map<TipoAtividade, Long> contarPorTipo(LocalDate inicio, LocalDate fim);

}
