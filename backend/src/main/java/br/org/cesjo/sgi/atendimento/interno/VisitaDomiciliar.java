package br.org.cesjo.sgi.atendimento.interno;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Visita domiciliar de avaliação de perfil (tabela {@code visita_domiciliar}, RF004, ADR 0070). Guarda
 * só o {@code pessoaId} e os {@code id} dos colaboradores.
 */
@Entity
@Table(name = "visita_domiciliar")
class VisitaDomiciliar {

	@Id
	private UUID id;

	@Column(name = "pessoa_id", nullable = false, updatable = false)
	private UUID pessoaId;

	@Column(name = "data_hora", nullable = false)
	private LocalDateTime dataHora;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusVisita status;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "visita_profissional", joinColumns = @JoinColumn(name = "visita_id"))
	@Column(name = "colaborador_id", nullable = false)
	private Set<UUID> profissionais = new LinkedHashSet<>();

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

	protected VisitaDomiciliar() {
	}

	/** Visita nova, agendada. */
	VisitaDomiciliar(UUID pessoaId, LocalDateTime dataHora, Set<UUID> profissionais, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.pessoaId = pessoaId;
		this.dataHora = dataHora;
		this.status = StatusVisita.AGENDADA;
		this.profissionais = new LinkedHashSet<>(profissionais);
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

	LocalDateTime getDataHora() {
		return dataHora;
	}

	StatusVisita getStatus() {
		return status;
	}

	Set<UUID> getProfissionais() {
		return Set.copyOf(profissionais);
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

}
