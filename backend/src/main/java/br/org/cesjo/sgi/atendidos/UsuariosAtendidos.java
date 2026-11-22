package br.org.cesjo.sgi.atendidos;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API pública do módulo Atendidos para os outros módulos (ADR 0081). A contagem que as vagas
 * disponíveis precisam (RF003): vagas = capacidade menos inserções ativas. E os usuários ativos, que as
 * atividades usam como participantes (RF010, ADR 0150).
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil. Só leitura de dados básicos, então não
 * audita.
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

	/** Usuários atendidos ativos, em ordem de nome. */
	List<UsuarioAtivo> listarAtivos();

	/**
	 * Confere que todas as {@code pessoaIds} são de usuários atendidos ativos.
	 * @throws br.org.cesjo.sgi.compartilhado.web.RegraNegocioException se alguma não for
	 */
	void exigirAtivos(Collection<UUID> pessoaIds);

}
