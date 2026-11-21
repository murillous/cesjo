package br.org.cesjo.sgi.atendimento.interno;

import java.time.Instant;
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
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Uma versão do PIA de uma pessoa (tabela {@code pia}, RF006, ADR 0103). O conteúdo é o JSON de
 * {@link ConteudoPia}, cifrado na aplicação.
 */
@Entity
@Table(name = "pia")
class Pia {

	@Id
	private UUID id;

	@Column(name = "pessoa_id", nullable = false, updatable = false)
	private UUID pessoaId;

	@Column(name = "numero_versao", nullable = false, updatable = false)
	private int numeroVersao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private StatusPia status;

	@Convert(converter = TextoCifradoConverter.class)
	@Column(nullable = false, columnDefinition = "text")
	private String conteudo;

	@Column(name = "versao_anterior_id", updatable = false)
	private UUID versaoAnteriorId;

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

	protected Pia() {
	}

	/** Versão nova, em elaboração. {@code anterior} é nulo na primeira versão. */
	Pia(UUID pessoaId, Pia anterior, String conteudoJson, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.pessoaId = pessoaId;
		this.numeroVersao = anterior == null ? 1 : anterior.numeroVersao + 1;
		this.versaoAnteriorId = anterior == null ? null : anterior.id;
		this.status = StatusPia.ELABORADO;
		this.conteudo = conteudoJson;
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	/** Troca o conteúdo. Só a versão em elaboração aceita edição. Devolve se mudou. */
	boolean editar(String conteudoJson, UUID autor, Instant agora) {
		if (status != StatusPia.ELABORADO) {
			throw new RegraNegocioException("Só um PIA em elaboração pode ser editado. Crie uma nova versão.");
		}
		if (conteudo.equals(conteudoJson)) {
			return false;
		}
		this.conteudo = conteudoJson;
		tocar(autor, agora);
		return true;
	}

	void tocar(UUID autor, Instant agora) {
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	UUID getPessoaId() {
		return pessoaId;
	}

	int getNumeroVersao() {
		return numeroVersao;
	}

	StatusPia getStatus() {
		return status;
	}

	void setStatus(StatusPia status) {
		this.status = status;
	}

	String getConteudo() {
		return conteudo;
	}

	UUID getVersaoAnteriorId() {
		return versaoAnteriorId;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

	Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
