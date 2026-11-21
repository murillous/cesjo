package br.org.cesjo.sgi.listaespera.interno;

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

import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;
import br.org.cesjo.sgi.listaespera.MotivoSaida;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;

/**
 * Uma mudança de situação da inscrição (tabela {@code historico_situacao_inscricao}, ADR 0091). Só
 * inserção: o histórico não é editado.
 */
@Entity
@Table(name = "historico_situacao_inscricao")
class HistoricoSituacao {

	@Id
	private UUID id;

	@Column(name = "inscricao_id", nullable = false, updatable = false)
	private UUID inscricaoId;

	@Enumerated(EnumType.STRING)
	@Column(name = "situacao_anterior", length = 30, updatable = false)
	private SituacaoInscricao situacaoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "situacao_nova", nullable = false, length = 30, updatable = false)
	private SituacaoInscricao situacaoNova;

	@Column(nullable = false, updatable = false)
	private LocalDate data;

	@Enumerated(EnumType.STRING)
	@Column(name = "motivo_saida", length = 30, updatable = false)
	private MotivoSaida motivoSaida;

	/** Cifrada: o motivo contado pela família pode ter dado pessoal. */
	@Column(updatable = false)
	@Convert(converter = TextoCifradoConverter.class)
	private String observacao;

	@Column(name = "registrado_por", updatable = false)
	private UUID registradoPor;

	@Column(name = "registrado_em", nullable = false, updatable = false)
	private Instant registradoEm;

	protected HistoricoSituacao() {
	}

	HistoricoSituacao(UUID inscricaoId, SituacaoInscricao anterior, SituacaoInscricao nova, LocalDate data,
			MotivoSaida motivo, String observacao, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.inscricaoId = inscricaoId;
		this.situacaoAnterior = anterior;
		this.situacaoNova = nova;
		this.data = data;
		this.motivoSaida = motivo;
		this.observacao = observacao;
		this.registradoPor = autor;
		this.registradoEm = agora;
	}

	UUID getId() {
		return id;
	}

	SituacaoInscricao getSituacaoAnterior() {
		return situacaoAnterior;
	}

	SituacaoInscricao getSituacaoNova() {
		return situacaoNova;
	}

	LocalDate getData() {
		return data;
	}

	MotivoSaida getMotivoSaida() {
		return motivoSaida;
	}

	String getObservacao() {
		return observacao;
	}

	Instant getRegistradoEm() {
		return registradoEm;
	}

}
