package br.org.cesjo.sgi.auditoria;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Uma operação a registrar no log de auditoria.
 *
 * <p>
 * Nunca leva valores: {@code camposAlterados} guarda só nomes de campos. {@code contaId} e
 * {@code login} só são preenchidos quando a conta não vem da requisição autenticada (login e falha de
 * login); nos demais casos a implementação usa {@code IdentidadeAtual}, de {@code compartilhado.seguranca}
 * (ADR 0009).
 *
 * @param acao tipo da operação
 * @param entidade nome da entidade afetada, por exemplo {@code conta_acesso} ou {@code arquivo}
 * @param entidadeId identificador do registro afetado, quando houver
 * @param camposAlterados nomes dos campos alterados, vazio quando não se aplica
 * @param contaId conta que fez a operação, quando não é a da requisição
 * @param login login informado, só para login e falha de login
 */
public record EventoAuditoria(AcaoAuditoria acao, String entidade, String entidadeId, Set<String> camposAlterados,
		UUID contaId, String login) {

	public EventoAuditoria {
		Objects.requireNonNull(acao, "acao");
		Objects.requireNonNull(entidade, "entidade");
		camposAlterados = camposAlterados == null ? Set.of() : Set.copyOf(camposAlterados);
	}

	public static EventoAuditoria criacao(String entidade, Object entidadeId) {
		return new EventoAuditoria(AcaoAuditoria.CRIAR, entidade, texto(entidadeId), Set.of(), null, null);
	}

	public static EventoAuditoria alteracao(String entidade, Object entidadeId, Set<String> camposAlterados) {
		return new EventoAuditoria(AcaoAuditoria.ALTERAR, entidade, texto(entidadeId), camposAlterados, null, null);
	}

	public static EventoAuditoria exclusao(String entidade, Object entidadeId) {
		return new EventoAuditoria(AcaoAuditoria.EXCLUIR, entidade, texto(entidadeId), Set.of(), null, null);
	}

	/** Leitura de registro com dado sensível (escuta, PIA, violação, laudo). */
	public static EventoAuditoria leituraSensivel(String entidade, Object entidadeId) {
		return new EventoAuditoria(AcaoAuditoria.LER_SENSIVEL, entidade, texto(entidadeId), Set.of(), null, null);
	}

	/** Exportação em PDF ou Excel. {@code entidade} nomeia o que foi exportado. */
	public static EventoAuditoria exportacao(String entidade) {
		return new EventoAuditoria(AcaoAuditoria.EXPORTAR, entidade, null, Set.of(), null, null);
	}

	/**
	 * Importação de uma planilha confirmada (ADR 0217). {@code entidade} nomeia o que foi importado; cada
	 * registro criado tem também o seu próprio evento de criação.
	 */
	public static EventoAuditoria importacao(String entidade) {
		return new EventoAuditoria(AcaoAuditoria.IMPORTAR, entidade, null, Set.of(), null, null);
	}

	public static EventoAuditoria login(UUID contaId, String login) {
		return new EventoAuditoria(AcaoAuditoria.LOGIN, "conta_acesso", texto(contaId), Set.of(), contaId, login);
	}

	public static EventoAuditoria loginFalho(String login) {
		return new EventoAuditoria(AcaoAuditoria.LOGIN_FALHO, "conta_acesso", null, Set.of(), null, login);
	}

	public static EventoAuditoria logout(UUID contaId, String login) {
		return new EventoAuditoria(AcaoAuditoria.LOGOUT, "conta_acesso", texto(contaId), Set.of(), contaId, login);
	}

	private static String texto(Object valor) {
		return valor == null ? null : valor.toString();
	}

}
