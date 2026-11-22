package br.org.cesjo.sgi.institucional.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;

/**
 * Um período de trabalho do colaborador no Centro-Dia, da contratação ao desligamento (tabela
 * {@code vinculo_colaborador}, RF014, ADR 0170). Sem data de desligamento, o vínculo está aberto.
 */
@Entity
@Table(name = "vinculo_colaborador")
class VinculoColaborador {

	@Id
	private UUID id;

	@Column(name = "colaborador_id", nullable = false, updatable = false)
	private UUID colaboradorId;

	@Column(name = "data_contratacao", nullable = false, updatable = false)
	private LocalDate dataContratacao;

	@Column(name = "data_desligamento")
	private LocalDate dataDesligamento;

	/** Cifrado: pode trazer detalhes pessoais e trabalhistas do colaborador. */
	@Column(name = "motivo_desligamento")
	@Convert(converter = TextoCifradoConverter.class)
	private String motivoDesligamento;

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

	protected VinculoColaborador() {
	}

	/** Vínculo novo, aberto. */
	VinculoColaborador(UUID colaboradorId, LocalDate dataContratacao, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.colaboradorId = colaboradorId;
		this.dataContratacao = dataContratacao;
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	void desligar(LocalDate data, String motivo, UUID autor, Instant agora) {
		this.dataDesligamento = data;
		this.motivoDesligamento = motivo.strip();
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	boolean isAberto() {
		return dataDesligamento == null;
	}

	UUID getId() {
		return id;
	}

	UUID getColaboradorId() {
		return colaboradorId;
	}

	LocalDate getDataContratacao() {
		return dataContratacao;
	}

	LocalDate getDataDesligamento() {
		return dataDesligamento;
	}

	String getMotivoDesligamento() {
		return motivoDesligamento;
	}

}
