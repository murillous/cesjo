package br.org.cesjo.sgi.listaespera;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

	/**
	 * Cadastra a pessoa e a inscreve na fila, aguardando, com a data de cadastro informada, como o
	 * cadastro pela tela (ADR 0215). Participa da transação de quem chama. Audita a criação da pessoa, da
	 * inscrição e do histórico.
	 *
	 * @return o {@code id} da inscrição
	 */
	UUID inscrever(NovoCandidato novo);

	/** Inscrição ativa da pessoa, se houver. */
	Optional<CandidatoAtivo> candidatoAtivo(UUID pessoaId);

	/**
	 * Marca o candidato como em avaliação, na mesma transação de quem chama. Não faz nada se ele já estiver.
	 * @throws br.org.cesjo.sgi.compartilhado.web.RegraNegocioException se a pessoa não tem inscrição ativa
	 */
	void colocarEmAvaliacao(UUID pessoaId);

	Optional<DadosInscricao> obter(UUID inscricaoId);

	/**
	 * Tira a inscrição da fila com a situação {@link SituacaoInscricao#INSERIDO} e grava a mudança no
	 * histórico de situação, com a data da inserção (ADR 0135). Aceita qualquer situação ativa, também
	 * o candidato em avaliação (ADR 0136). Participa da transação de quem chama: se a inserção falhar
	 * depois, a inscrição volta à situação de antes.
	 *
	 * @throws br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException quando a inscrição não
	 * existe
	 * @throws br.org.cesjo.sgi.compartilhado.web.RegraNegocioException quando a inscrição não está na
	 * fila
	 */
	DadosInscricao registrarInsercao(UUID inscricaoId, LocalDate dataInsercao);

	/**
	 * Inscrições com data de cadastro entre {@code inicio} e {@code fim}, inclusive (RF016, ADR 0200).
	 * Só conta, sem identificar ninguém, então não audita.
	 */
	long contarInscricoes(LocalDate inicio, LocalDate fim);

	/** Inscrições na fila (aguardando ou em avaliação) no fim de {@code dia}, pelo histórico (ADR 0200). */
	long contarNaFilaEm(LocalDate dia);

	/**
	 * Saídas da lista (situação "saiu") com data entre {@code inicio} e {@code fim}, inclusive, por
	 * motivo (ADR 0200). Todo motivo aparece, com zero quando não houve.
	 */
	Map<MotivoSaida, Long> contarSaidasPorMotivo(LocalDate inicio, LocalDate fim);

}
