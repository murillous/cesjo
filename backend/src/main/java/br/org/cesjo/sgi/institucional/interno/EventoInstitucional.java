package br.org.cesjo.sgi.institucional.interno;

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

import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;
import br.org.cesjo.sgi.institucional.DirecaoEvento;
import br.org.cesjo.sgi.institucional.TipoEventoInstitucional;

/** Reunião, formação, visita ou evento institucional (tabela {@code evento_institucional}, RF012, ADR 0175). */
@Entity
@Table(name = "evento_institucional")
class EventoInstitucional {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private TipoEventoInstitucional tipo;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private DirecaoEvento direcao;

	@Column(nullable = false)
	private LocalDate data;

	/** Cifrada: reuniões da equipe podem discutir casos de usuários atendidos. */
	@Column(nullable = false)
	@Convert(converter = TextoCifradoConverter.class)
	private String descricao;

	@Column(length = 2000)
	private String participantes;

	@Column(name = "parceiro_id")
	private UUID parceiroId;

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

	protected EventoInstitucional() {
	}

	EventoInstitucional(DadosEvento dados, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.criadoEm = agora;
		this.criadoPor = autor;
		aplicar(dados, autor, agora);
	}

	void aplicar(DadosEvento dados, UUID autor, Instant agora) {
		this.tipo = dados.tipo();
		this.direcao = dados.direcao();
		this.data = dados.data();
		this.descricao = dados.descricao().strip();
		this.participantes = dados.participantesNormalizados();
		this.parceiroId = dados.parceiroId();
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	TipoEventoInstitucional getTipo() {
		return tipo;
	}

	DirecaoEvento getDirecao() {
		return direcao;
	}

	LocalDate getData() {
		return data;
	}

	String getDescricao() {
		return descricao;
	}

	String getParticipantes() {
		return participantes;
	}

	UUID getParceiroId() {
		return parceiroId;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

	Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
