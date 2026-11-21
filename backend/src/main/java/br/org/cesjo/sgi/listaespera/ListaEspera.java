package br.org.cesjo.sgi.listaespera;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública da lista de espera (ADR 0049), usada pelo agendamento de visitas domiciliares (RF004).
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil. A auditoria das mudanças é daqui.
 */
public interface ListaEspera {

	/** Candidatos ativos ({@link SituacaoInscricao#ATIVAS}), na ordem da fila. */
	List<CandidatoAtivo> listarCandidatosAtivos();

	/** Inscrição ativa da pessoa, se houver. */
	Optional<CandidatoAtivo> candidatoAtivo(UUID pessoaId);

	/**
	 * Marca o candidato como em avaliação, na mesma transação de quem chama. Não faz nada se ele já estiver.
	 * @throws br.org.cesjo.sgi.compartilhado.web.RegraNegocioException se a pessoa não tem inscrição ativa
	 */
	void colocarEmAvaliacao(UUID pessoaId);

}
