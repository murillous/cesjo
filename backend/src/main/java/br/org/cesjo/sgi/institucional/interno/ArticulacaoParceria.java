package br.org.cesjo.sgi.institucional.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Ação de articulação ou fortalecimento de uma parceria, com data e descrição (tabela
 * {@code articulacao_parceria}, RF013, ADR 0172).
 */
@Entity
@Table(name = "articulacao_parceria")
class ArticulacaoParceria {

	@Id
	private UUID id;

	@Column(name = "parceiro_id", nullable = false, updatable = false)
	private UUID parceiroId;

	@Column(nullable = false, updatable = false)
	private LocalDate data;

	@Column(nullable = false, length = 2000, updatable = false)
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

	protected ArticulacaoParceria() {
	}

	ArticulacaoParceria(UUID parceiroId, LocalDate data, String descricao, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.parceiroId = parceiroId;
		this.data = data;
		this.descricao = descricao.strip();
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	LocalDate getData() {
		return data;
	}

	String getDescricao() {
		return descricao;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

}
