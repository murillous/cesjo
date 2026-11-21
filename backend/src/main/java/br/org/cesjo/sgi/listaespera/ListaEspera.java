package br.org.cesjo.sgi.listaespera;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública do módulo Lista de espera para os outros módulos (ADR 0049 e ADR 0081). O agendamento de
 * visitas domiciliares (RF004) marca o candidato como em avaliação, e o módulo Atendidos tira a
 * inscrição da fila, sem tocar a tabela.
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil. A auditoria das mudanças é daqui.
 */
public interface ListaEspera {

	/** Nome da entidade inscrição no log de auditoria. */
	String ENTIDADE = "inscricao_lista_espera";

	/** Candidatos ativos ({@link SituacaoInscricao#ATIVAS}), na ordem da fila. */
	List<CandidatoAtivo> listarCandidatosAtivos();

	/** Inscrição ativa da pessoa, se houver. */
	Optional<CandidatoAtivo> candidatoAtivo(UUID pessoaId);

	/**
	 * Marca o candidato como em avaliação, na mesma transação de quem chama. Não faz nada se ele já estiver.
	 * @throws br.org.cesjo.sgi.compartilhado.web.RegraNegocioException se a pessoa não tem inscrição ativa
	 */
	void colocarEmAvaliacao(UUID pessoaId);

	Optional<DadosInscricao> obter(UUID inscricaoId);

	/**
	 * Tira a inscrição da fila com a situação {@link SituacaoInscricao#INSERIDO}. Participa da transação
	 * de quem chama: se a inserção falhar depois, a inscrição volta a aguardar.
	 *
	 * @throws br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException quando a inscrição não
	 * existe
	 * @throws br.org.cesjo.sgi.compartilhado.web.RegraNegocioException quando a inscrição não está
	 * aguardando
	 */
	DadosInscricao registrarInsercao(UUID inscricaoId);

}
