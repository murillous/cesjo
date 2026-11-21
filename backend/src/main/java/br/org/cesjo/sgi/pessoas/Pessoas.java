package br.org.cesjo.sgi.pessoas;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública do módulo Pessoas (ADR 0037). Os outros módulos (lista de espera, atendidos) guardam só o
 * {@code id} da pessoa e leem os dados por aqui.
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil. A auditoria é daqui.
 */
public interface Pessoas {

	/** Nome da entidade pessoa no log de auditoria. */
	String ENTIDADE = "pessoa";

	/** Nome da entidade responsável no log de auditoria. */
	String ENTIDADE_RESPONSAVEL = "responsavel";

	/** Cadastra a pessoa e o seu responsável e devolve o {@code id} da pessoa. */
	UUID cadastrar(NovaPessoa nova);

	Optional<DadosPessoa> obter(UUID id);

	/** Nome completo de cada pessoa encontrada, pelo {@code id}. Ids inexistentes ficam de fora. */
	Map<UUID, String> nomes(Collection<UUID> ids);

}
