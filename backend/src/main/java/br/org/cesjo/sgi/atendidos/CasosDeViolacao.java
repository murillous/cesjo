package br.org.cesjo.sgi.atendidos;

import java.time.LocalDate;
import java.util.List;

/**
 * API pública dos casos de violação de direitos por período, para o relatório de intervenção (RF016,
 * ADR 0205).
 *
 * <p>
 * Devolve dado pessoal sensível: o nome de quem sofreu a violação e a categoria. A permissão e a
 * auditoria são de quem chama, que precisa checar {@code VIOLACOES_LER} e registrar a leitura ou a
 * exportação. A descrição da violação não sai por aqui.
 */
public interface CasosDeViolacao {

	/**
	 * Usuários com violação identificada entre {@code inicio} e {@code fim}, as duas inclusive, em ordem
	 * de nome, cada um com as violações do período e a situação no serviço no fim do período.
	 *
	 * @throws br.org.cesjo.sgi.compartilhado.web.RegraNegocioException quando {@code fim} é anterior a
	 * {@code inicio}
	 */
	List<CasoDeViolacao> casosNoPeriodo(LocalDate inicio, LocalDate fim);

}
