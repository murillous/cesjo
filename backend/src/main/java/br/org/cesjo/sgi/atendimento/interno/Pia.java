package br.org.cesjo.sgi.atendimento.interno;

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

	@Column(name = "apresentado_familia_em")
	private LocalDate apresentadoFamiliaEm;

	@Column(name = "apresentado_equipe_em")
	private LocalDate apresentadoEquipeEm;

	@Column(name = "aprovado_em")
	private LocalDate aprovadoEm;

	@Column(name = "aprovado_por")
	private UUID aprovadoPor;

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

	/**
	 * Avança para o próximo status, na ordem do RF006 (ADR 0106), com a data do passo. A data não pode ser
	 * anterior à do passo de antes. Devolve o nome da coluna da data.
	 */
	String avancar(StatusPia novo, LocalDate data, UUID autor, Instant agora) {
		StatusPia esperado = StatusPia.values()[Math.min(status.ordinal() + 1, StatusPia.values().length - 1)];
		if (status == StatusPia.APROVADO || novo != esperado) {
			throw new RegraNegocioException(switch (status) {
				case ELABORADO -> "O PIA em elaboração precisa ser apresentado à família primeiro.";
				case APRESENTADO_FAMILIA -> "O PIA apresentado à família precisa ser apresentado à equipe antes.";
				case APRESENTADO_EQUIPE -> "O PIA apresentado à equipe só pode ser aprovado.";
				case APROVADO -> "O PIA já está aprovado. Para mudar, crie uma nova versão.";
			});
		}
		LocalDate anterior = switch (status) {
			case APRESENTADO_FAMILIA -> apresentadoFamiliaEm;
			case APRESENTADO_EQUIPE -> apresentadoEquipeEm;
			default -> null;
		};
		if (anterior != null && data.isBefore(anterior)) {
			throw new RegraNegocioException("A data não pode ser anterior à do passo anterior do PIA.");
		}
		String coluna = switch (novo) {
			case APRESENTADO_FAMILIA -> {
				apresentadoFamiliaEm = data;
				yield "apresentado_familia_em";
			}
			case APRESENTADO_EQUIPE -> {
				apresentadoEquipeEm = data;
				yield "apresentado_equipe_em";
			}
			case APROVADO -> {
				aprovadoEm = data;
				aprovadoPor = autor;
				yield "aprovado_em";
			}
			case ELABORADO -> throw new IllegalStateException();
		};
		this.status = novo;
		tocar(autor, agora);
		return coluna;
	}

	LocalDate getApresentadoFamiliaEm() {
		return apresentadoFamiliaEm;
	}

	LocalDate getApresentadoEquipeEm() {
		return apresentadoEquipeEm;
	}

	LocalDate getAprovadoEm() {
		return aprovadoEm;
	}

	UUID getAprovadoPor() {
		return aprovadoPor;
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
