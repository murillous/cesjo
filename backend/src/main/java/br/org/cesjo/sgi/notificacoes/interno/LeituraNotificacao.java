package br.org.cesjo.sgi.notificacoes.interno;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Registro de que uma conta leu uma notificação (ADR 0030). Gravado só por
 * {@link LeituraNotificacaoRepositorio#registrar}; a entidade existe para as consultas JPQL.
 */
@Entity
@Table(name = "notificacao_leitura")
@IdClass(LeituraNotificacao.Chave.class)
class LeituraNotificacao {

	@Id
	@Column(name = "notificacao_id")
	private UUID notificacaoId;

	@Id
	@Column(name = "conta_id")
	private UUID contaId;

	@Column(name = "lida_em")
	private Instant lidaEm;

	protected LeituraNotificacao() {
	}

	record Chave(UUID notificacaoId, UUID contaId) implements Serializable {
	}

}
