package br.org.cesjo.sgi.listaespera.interno;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uma definição da capacidade do serviço (tabela {@code capacidade_servico}, ADR 0092). Só inserção:
 * mudar a capacidade grava uma linha nova, e a vigente é a mais recente.
 */
@Entity
@Table(name = "capacidade_servico")
class CapacidadeServico {

	@Id
	private UUID id;

	@Column(nullable = false, updatable = false)
	private int vagas;

	@Column(name = "definida_por", updatable = false)
	private UUID definidaPor;

	@Column(name = "definida_em", nullable = false, updatable = false)
	private Instant definidaEm;

	protected CapacidadeServico() {
	}

	CapacidadeServico(int vagas, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.vagas = vagas;
		this.definidaPor = autor;
		this.definidaEm = agora;
	}

	UUID getId() {
		return id;
	}

	int getVagas() {
		return vagas;
	}

	Instant getDefinidaEm() {
		return definidaEm;
	}

}
