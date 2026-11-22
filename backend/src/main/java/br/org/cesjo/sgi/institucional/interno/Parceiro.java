package br.org.cesjo.sgi.institucional.interno;

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

import br.org.cesjo.sgi.institucional.TipoParceiro;

/** Instituição parceira do Centro-Dia (tabela {@code parceiro}, RF013, ADR 0172). */
@Entity
@Table(name = "parceiro")
class Parceiro {

	@Id
	private UUID id;

	@Column(nullable = false, length = 150)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private TipoParceiro tipo;

	/** Termo, convênio, acordo ou outro instrumento da parceria. */
	@Column(length = 200)
	private String instrumento;

	@Column(name = "vigencia_inicio")
	private LocalDate vigenciaInicio;

	@Column(name = "vigencia_fim")
	private LocalDate vigenciaFim;

	@Column(length = 500)
	private String contato;

	@Column(length = 2000)
	private String observacoes;

	@Column(nullable = false)
	private boolean ativo;

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

	protected Parceiro() {
	}

	/** Parceiro novo, sempre ativo. */
	Parceiro(DadosParceiro dados, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.ativo = true;
		this.criadoEm = agora;
		this.criadoPor = autor;
		aplicar(dados, autor, agora);
	}

	/** Troca os dados cadastrais. */
	void aplicar(DadosParceiro dados, UUID autor, Instant agora) {
		this.nome = dados.nome().strip();
		this.tipo = dados.tipo();
		this.instrumento = DadosParceiro.texto(dados.instrumento());
		this.vigenciaInicio = dados.vigenciaInicio();
		this.vigenciaFim = dados.vigenciaFim();
		this.contato = DadosParceiro.texto(dados.contato());
		this.observacoes = DadosParceiro.texto(dados.observacoes());
		tocar(autor, agora);
	}

	void alterarAtivo(boolean ativo, UUID autor, Instant agora) {
		this.ativo = ativo;
		tocar(autor, agora);
	}

	private void tocar(UUID autor, Instant agora) {
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	String getNome() {
		return nome;
	}

	TipoParceiro getTipo() {
		return tipo;
	}

	String getInstrumento() {
		return instrumento;
	}

	LocalDate getVigenciaInicio() {
		return vigenciaInicio;
	}

	LocalDate getVigenciaFim() {
		return vigenciaFim;
	}

	String getContato() {
		return contato;
	}

	String getObservacoes() {
		return observacoes;
	}

	boolean isAtivo() {
		return ativo;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

	Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
