package br.org.cesjo.sgi.atendidos;

import java.util.Map;

/**
 * API pública do módulo Atendidos para os outros módulos (ADR 0081). Por enquanto só a contagem que
 * as vagas disponíveis precisam (RF003): vagas = capacidade menos inserções ativas.
 */
public interface UsuariosAtendidos {

	/** Nome da entidade inserção no log de auditoria. */
	String ENTIDADE = "insercao";

	/** Total de inserções ativas, que ocupam vaga. */
	long contarAtivos();

	/**
	 * Inserções ativas por turno, para quando a capacidade for por turno (pergunta P3). Turno sem
	 * inserção aparece com zero.
	 */
	Map<Turno, Long> contarAtivosPorTurno();

}
