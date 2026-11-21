package br.org.cesjo.sgi.institucional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública dos colaboradores (RF014 parcial, ADR 0045). Visitas domiciliares, escutas e atividades
 * guardam só o {@code id} do colaborador e usam esta interface para listar quem pode ser escolhido e
 * validar a escolha.
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil. Só leitura, então não audita.
 */
public interface Colaboradores {

	/** Nome da entidade colaborador no log de auditoria. */
	String ENTIDADE = "colaborador";

	/** Colaboradores ativos, em ordem de nome. */
	List<ResumoColaborador> listarAtivos();

	/** O colaborador, se existir e estiver ativo. */
	Optional<ResumoColaborador> obterAtivo(UUID id);

	/**
	 * Confere que todos os {@code ids} são de colaboradores ativos.
	 * @throws ColaboradorIndisponivelException se algum não existir ou estiver inativo
	 */
	void exigirAtivos(Collection<UUID> ids);

	/** Colaborador ativo ligado à conta de acesso, por exemplo para sugerir o profissional da escuta. */
	Optional<ResumoColaborador> obterAtivoPorConta(UUID contaId);

}
