package br.org.cesjo.sgi.atendimento.interno;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;

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

	@Column(name = "motivo_cancelamento", length = 500)
	private String motivoCancelamento;

	/** Texto livre sensível, cifrado na aplicação (ADR 0004, ADR 0073). */
	@Convert(converter = TextoCifradoConverter.class)
	@Column(columnDefinition = "text")
	private String observacoes;

	/** Visita cancelada que esta reagenda, se houver. */
	@Column(name = "visita_origem_id", updatable = false)
	private UUID visitaOrigemId;

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

	/** Visita nova que reagenda {@code origem}, já cancelada. */
	static VisitaDomiciliar reagendamento(VisitaDomiciliar origem, LocalDateTime dataHora, Set<UUID> profissionais,
			UUID autor, Instant agora) {
		VisitaDomiciliar nova = new VisitaDomiciliar(origem.pessoaId, dataHora, profissionais, autor, agora);
		nova.visitaOrigemId = origem.id;
		return nova;
	}

	/** Marca como realizada, com observações opcionais. */
	void realizar(String observacoes, UUID autor, Instant agora) {
		exigirAgendada();
		this.status = StatusVisita.REALIZADA;
		this.observacoes = observacoes;
		tocar(autor, agora);
	}

	void cancelar(String motivo, UUID autor, Instant agora) {
		exigirAgendada();
		this.status = StatusVisita.CANCELADA;
		this.motivoCancelamento = motivo;
		tocar(autor, agora);
	}

	private void exigirAgendada() {
		if (status != StatusVisita.AGENDADA) {
			throw new br.org.cesjo.sgi.compartilhado.web.RegraNegocioException(
					"Só uma visita agendada pode ser realizada, cancelada ou reagendada.");
		}
	}

	private void tocar(UUID autor, Instant agora) {
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	String getMotivoCancelamento() {
		return motivoCancelamento;
	}

	String getObservacoes() {
		return observacoes;
	}

	UUID getVisitaOrigemId() {
		return visitaOrigemId;
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
