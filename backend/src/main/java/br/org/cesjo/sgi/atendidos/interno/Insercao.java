package br.org.cesjo.sgi.atendidos.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.atendidos.SituacaoInsercao;
import br.org.cesjo.sgi.atendidos.Turno;

/**
 * Inserção de uma pessoa no serviço (tabela {@code insercao}, ADR 0080). É o que faz dela um usuário
 * atendido. Os dados da pessoa, da inscrição e do cuidador ficam nos seus módulos; aqui só os ids.
 */
@Entity
@Table(name = "insercao")
class Insercao {

	@Id
	private UUID id;

	@Column(name = "pessoa_id", nullable = false, updatable = false)
	private UUID pessoaId;

	@Column(name = "inscricao_id", nullable = false, updatable = false)
	private UUID inscricaoId;

	@Column(name = "data_insercao", nullable = false)
	private LocalDate dataInsercao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Turno turno;

	@Column(name = "cuidador_responsavel_id", nullable = false)
	private UUID cuidadorResponsavelId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SituacaoInsercao situacao;

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

	protected Insercao() {
	}

	/** Inserção nova, ativa. */
	Insercao(UUID pessoaId, UUID inscricaoId, LocalDate dataInsercao, Turno turno, UUID cuidadorResponsavelId,
			UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.pessoaId = pessoaId;
		this.inscricaoId = inscricaoId;
		this.dataInsercao = dataInsercao;
		this.turno = turno;
		this.cuidadorResponsavelId = cuidadorResponsavelId;
		this.situacao = SituacaoInsercao.ATIVA;
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	/** Tira do serviço: a inserção deixa de ocupar vaga (RF008). */
	void desligar(UUID autor, Instant agora) {
		if (situacao != SituacaoInsercao.ATIVA) {
			throw new IllegalStateException("Inserção já desligada: " + id);
		}
		this.situacao = SituacaoInsercao.DESLIGADA;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	UUID getPessoaId() {
		return pessoaId;
	}

	UUID getInscricaoId() {
		return inscricaoId;
	}

	LocalDate getDataInsercao() {
		return dataInsercao;
	}

	Turno getTurno() {
		return turno;
	}

	UUID getCuidadorResponsavelId() {
		return cuidadorResponsavelId;
	}

	SituacaoInsercao getSituacao() {
		return situacao;
	}

}
