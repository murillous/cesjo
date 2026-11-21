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

import br.org.cesjo.sgi.atendidos.MotivoDesligamento;
import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;

/** Desligamento de um usuário atendido (tabela {@code desligamento}, RF008, ADR 0110). */
@Entity
@Table(name = "desligamento")
class Desligamento {

	@Id
	private UUID id;

	@Column(name = "insercao_id", nullable = false, updatable = false)
	private UUID insercaoId;

	@Column(nullable = false)
	private LocalDate data;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MotivoDesligamento motivo;

	/** Cifrada: pode trazer detalhes da família e da saúde. */
	@Column
	@Convert(converter = TextoCifradoConverter.class)
	private String observacao;

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

	protected Desligamento() {
	}

	Desligamento(UUID insercaoId, LocalDate data, MotivoDesligamento motivo, String observacao, UUID autor,
			Instant agora) {
		this.id = UUID.randomUUID();
		this.insercaoId = insercaoId;
		this.data = data;
		this.motivo = motivo;
		this.observacao = observacao;
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	UUID getInsercaoId() {
		return insercaoId;
	}

	LocalDate getData() {
		return data;
	}

	MotivoDesligamento getMotivo() {
		return motivo;
	}

	String getObservacao() {
		return observacao;
	}

}
