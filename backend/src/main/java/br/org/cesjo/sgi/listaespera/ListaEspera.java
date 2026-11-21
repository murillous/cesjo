package br.org.cesjo.sgi.listaespera;

import java.util.Optional;
import java.util.UUID;

/**
 * API pública do módulo Lista de espera para os outros módulos (ADR 0081). O módulo Atendidos tira a
 * inscrição da fila por aqui, sem tocar a tabela.
 *
 * <p>
 * A permissão é de quem chama. A auditoria é daqui.
 */
public interface ListaEspera {

	/** Nome da entidade inscrição no log de auditoria. */
	String ENTIDADE = "inscricao_lista_espera";

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
