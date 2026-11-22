package br.org.cesjo.sgi.atividades;

import java.time.LocalDate;
import java.util.Map;

/**
 * API pública dos contatos e comunicações do RF011 para os outros módulos (ADR 0161). O relatório
 * mensal (issue #58) consolida os quantitativos por tipo sem conhecer a tabela {@code comunicacao}.
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil. Só conta, sem expor contraparte, assunto
 * nem descrição, então não audita.
 */
public interface Comunicacoes {

	/** Nome da entidade comunicação no log de auditoria. */
	String ENTIDADE = "comunicacao";

	/**
	 * Comunicações com data entre {@code inicio} e {@code fim}, as duas inclusive, contadas por tipo. Todo
	 * tipo aparece no mapa, com zero quando não houve registro.
	 *
	 * @throws IllegalArgumentException quando {@code fim} é anterior a {@code inicio}
	 */
	Map<TipoComunicacao, Long> contarPorTipo(LocalDate inicio, LocalDate fim);

	/**
	 * Ofícios, declarações ou encaminhamentos ({@code tipo}) com data no período, as duas pontas inclusive,
	 * contados por direção: expedidos ou enviados e recebidos. As duas direções aparecem no mapa.
	 *
	 * @throws IllegalArgumentException quando {@code fim} é anterior a {@code inicio} ou quando o tipo não
	 * tem direção ({@link TipoComunicacao#temDirecao()})
	 */
	Map<DirecaoComunicacao, Long> contarPorDirecao(TipoComunicacao tipo, LocalDate inicio, LocalDate fim);

}
