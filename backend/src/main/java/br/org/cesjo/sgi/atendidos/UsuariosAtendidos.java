package br.org.cesjo.sgi.atendidos;

import java.time.LocalDate;
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

	/**
	 * Insere no serviço o candidato da fila com o responsável do cadastro (RF001) como cuidador familiar,
	 * com as mesmas regras da inserção pela tela (ADR 0080). Usado pela importação de planilha (ADR 0220).
	 * Participa da transação de quem chama e audita a criação.
	 *
	 * @return o {@code id} da inserção
	 * @throws br.org.cesjo.sgi.compartilhado.web.RegraNegocioException quando a inscrição não está na
	 * fila, a data é anterior ao cadastro ou a pessoa já é usuária ativa
	 */
	UUID inserirComOResponsavel(UUID inscricaoId, LocalDate dataInsercao, Turno turno);

	/** Total de inserções ativas, que ocupam vaga. */
	long contarAtivos();

	/**
	 * Inserções ativas por turno, para quando a capacidade for por turno (pergunta P3). Turno sem
	 * inserção aparece com zero.
	 */
	Map<Turno, Long> contarAtivosPorTurno();

	/**
	 * Perfil de cada usuário com inserção ativa, para os dashboards contarem por deficiência, gênero,
	 * faixa etária e turno (RF017, ADR 0195). Lido na hora, sem cache. Não é auditado: não identifica a
	 * pessoa.
	 */
	List<PerfilAtendido> perfisDosAtivos();
	/** Usuários atendidos ativos, em ordem de nome. */
	List<UsuarioAtivo> listarAtivos();

	/**
	 * Confere que todas as {@code pessoaIds} são de usuários atendidos ativos.
	 * @throws br.org.cesjo.sgi.compartilhado.web.RegraNegocioException se alguma não for
	 */
	void exigirAtivos(Collection<UUID> pessoaIds);

	/**
	 * Violações com data de identificação entre {@code inicio} e {@code fim}, inclusive, contadas por
	 * categoria (RF017, ADR 0198). Toda categoria aparece, com zero quando não houve. Só conta, sem
	 * identificar a pessoa, então não audita.
	 */
	Map<TipoViolacao, Long> contarViolacoesPorTipo(LocalDate inicio, LocalDate fim);

	/**
	 * Desligamentos com data entre {@code inicio} e {@code fim}, inclusive, contados por motivo (RF017,
	 * ADR 0198). Todo motivo aparece, com zero quando não houve.
	 */
	Map<MotivoDesligamento, Long> contarDesligamentosPorMotivo(LocalDate inicio, LocalDate fim);

	/**
	 * Usuários atendidos em algum dia entre {@code inicio} e {@code fim}: inseridos até o fim e não
	 * desligados antes do início (ADR 0198). É a base da taxa de desligamento.
	 */
	long contarAtendidosNoPeriodo(LocalDate inicio, LocalDate fim);

	/** Inserções no serviço com data entre {@code inicio} e {@code fim}, inclusive (RF016, ADR 0200). */
	long contarInsercoes(LocalDate inicio, LocalDate fim);

	/** Usuários com inserção ativa no fim de {@code dia}: inseridos até o dia e não desligados até ele. */
	long contarAtivosEm(LocalDate dia);

}
