package br.org.cesjo.sgi.auditoria.interno;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.org.cesjo.sgi.auditoria.AcaoAuditoria;

/**
 * Uma linha do log de auditoria. É imutável: só é inserida, nunca alterada nem excluída (ADR 0022).
 * Guarda nomes de campos, nunca valores (ADR 0020).
 */
@Entity
@Immutable
@Table(name = "auditoria")
class RegistroAuditoria {

	@Id
	private UUID id;

	@Column(name = "ocorrido_em", nullable = false)
	private Instant ocorridoEm;

	@Column(name = "conta_id")
	private UUID contaId;

	/** Retrato do login da conta no momento da operação; o módulo não lê as tabelas de acesso. */
	@Column(name = "conta_login")
	private String contaLogin;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AcaoAuditoria acao;

	@Column(nullable = false)
	private String entidade;

	@Column(name = "entidade_id")
	private String entidadeId;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "campos_alterados", nullable = false)
	private String[] camposAlterados;

	private String ip;

	protected RegistroAuditoria() {
	}

	RegistroAuditoria(Instant ocorridoEm, UUID contaId, String contaLogin, AcaoAuditoria acao, String entidade,
			String entidadeId, List<String> camposAlterados, String ip) {
		this.id = UUID.randomUUID();
		this.ocorridoEm = ocorridoEm;
		this.contaId = contaId;
		this.contaLogin = contaLogin;
		this.acao = acao;
		this.entidade = entidade;
		this.entidadeId = entidadeId;
		this.camposAlterados = camposAlterados.toArray(String[]::new);
		this.ip = ip;
	}

	UUID id() {
		return id;
	}

	Instant ocorridoEm() {
		return ocorridoEm;
	}

	UUID contaId() {
		return contaId;
	}

	String contaLogin() {
		return contaLogin;
	}

	AcaoAuditoria acao() {
		return acao;
	}

	String entidade() {
		return entidade;
	}

	String entidadeId() {
		return entidadeId;
	}

	List<String> camposAlterados() {
		return List.of(camposAlterados);
	}

	String ip() {
		return ip;
	}

}
