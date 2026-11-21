package br.org.cesjo.sgi.pessoas.interno;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Responsável pela pessoa (tabela {@code responsavel}, ADR 0037). No RF001 só o nome é obrigatório;
 * parentesco, telefone e cuidador principal entram com o cadastro completo (RF007, pergunta 7).
 */
@Entity
@Table(name = "responsavel")
class Responsavel {

	@Id
	private UUID id;

	@Column(name = "pessoa_id", nullable = false, updatable = false)
	private UUID pessoaId;

	@Column(name = "nome_completo", nullable = false, length = 150)
	private String nomeCompleto;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "criado_por", updatable = false)
	private UUID criadoPor;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	@Column(name = "atualizado_por")
	private UUID atualizadoPor;

	@Version
	private long versao;

	protected Responsavel() {
	}

	Responsavel(UUID pessoaId, String nomeCompleto, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.pessoaId = pessoaId;
		this.nomeCompleto = nomeCompleto;
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	String getNomeCompleto() {
		return nomeCompleto;
	}

}
