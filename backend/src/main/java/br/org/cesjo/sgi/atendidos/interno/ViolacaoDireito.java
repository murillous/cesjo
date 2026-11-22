package br.org.cesjo.sgi.atendidos.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.atendidos.AmbitoViolacao;
import br.org.cesjo.sgi.atendidos.TipoViolacao;
import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;

/** Violação de direitos identificada (tabela {@code violacao_direito}, RF009, ADR 0112). */
@Entity
@Table(name = "violacao_direito")
class ViolacaoDireito {

	@Id
	private UUID id;

	@Column(name = "pessoa_id", nullable = false, updatable = false)
	private UUID pessoaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private TipoViolacao tipo;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private AmbitoViolacao ambito;

	@Column(name = "data_identificacao", nullable = false)
	private LocalDate dataIdentificacao;

	/** Cifrada; ler é auditado (ADR 0145). */
	@Column(nullable = false)
	@Convert(converter = TextoCifradoConverter.class)
	private String descricao;

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

	protected ViolacaoDireito() {
	}

	ViolacaoDireito(UUID pessoaId, TipoViolacao tipo, AmbitoViolacao ambito, LocalDate dataIdentificacao,
			String descricao, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.pessoaId = pessoaId;
		this.tipo = tipo;
		this.ambito = ambito;
		this.dataIdentificacao = dataIdentificacao;
		this.descricao = descricao;
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	UUID getPessoaId() {
		return pessoaId;
	}

	TipoViolacao getTipo() {
		return tipo;
	}

	AmbitoViolacao getAmbito() {
		return ambito;
	}

	LocalDate getDataIdentificacao() {
		return dataIdentificacao;
	}

	String getDescricao() {
		return descricao;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

	UUID getCriadoPor() {
		return criadoPor;
	}

}
