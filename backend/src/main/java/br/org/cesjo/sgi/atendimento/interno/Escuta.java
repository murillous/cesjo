package br.org.cesjo.sgi.atendimento.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;

/**
 * Escuta ativa e qualificada (tabela {@code escuta}, RF005, ADR 0100). Os quatro textos são cifrados na
 * aplicação.
 */
@Entity
@Table(name = "escuta")
class Escuta {

	@Id
	private UUID id;

	@Column(name = "pessoa_id", nullable = false, updatable = false)
	private UUID pessoaId;

	@Column(name = "data_escuta", nullable = false)
	private LocalDate dataEscuta;

	/** Profissional responsável. */
	@Column(name = "colaborador_id", nullable = false)
	private UUID profissionalId;

	@Convert(converter = TextoCifradoConverter.class)
	@Column(name = "dinamica_familiar", columnDefinition = "text")
	private String dinamicaFamiliar;

	@Convert(converter = TextoCifradoConverter.class)
	@Column(name = "necessidades_imediatas", columnDefinition = "text")
	private String necessidadesImediatas;

	@Convert(converter = TextoCifradoConverter.class)
	@Column(columnDefinition = "text")
	private String habilidades;

	@Convert(converter = TextoCifradoConverter.class)
	@Column(columnDefinition = "text")
	private String potencialidades;

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

	protected Escuta() {
	}

	Escuta(UUID pessoaId, DadosEscuta dados, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.pessoaId = pessoaId;
		aplicar(dados);
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	/** Aplica a correção e devolve os nomes das colunas que mudaram. */
	Set<String> corrigir(DadosEscuta dados, UUID autor, Instant agora) {
		Set<String> alterados = new LinkedHashSet<>();
		if (!dataEscuta.equals(dados.dataEscuta())) {
			alterados.add("data_escuta");
		}
		if (!profissionalId.equals(dados.profissionalId())) {
			alterados.add("colaborador_id");
		}
		if (!Objects.equals(dinamicaFamiliar, limpo(dados.dinamicaFamiliar()))) {
			alterados.add("dinamica_familiar");
		}
		if (!Objects.equals(necessidadesImediatas, limpo(dados.necessidadesImediatas()))) {
			alterados.add("necessidades_imediatas");
		}
		if (!Objects.equals(habilidades, limpo(dados.habilidades()))) {
			alterados.add("habilidades");
		}
		if (!Objects.equals(potencialidades, limpo(dados.potencialidades()))) {
			alterados.add("potencialidades");
		}
		if (!alterados.isEmpty()) {
			aplicar(dados);
			this.atualizadoEm = agora;
			this.atualizadoPor = autor;
		}
		return alterados;
	}

	private void aplicar(DadosEscuta dados) {
		this.dataEscuta = dados.dataEscuta();
		this.profissionalId = dados.profissionalId();
		this.dinamicaFamiliar = limpo(dados.dinamicaFamiliar());
		this.necessidadesImediatas = limpo(dados.necessidadesImediatas());
		this.habilidades = limpo(dados.habilidades());
		this.potencialidades = limpo(dados.potencialidades());
	}

	/** Texto sem espaços nas pontas; vazio vira nulo. */
	static String limpo(String texto) {
		return texto == null || texto.isBlank() ? null : texto.strip();
	}

	UUID getId() {
		return id;
	}

	UUID getPessoaId() {
		return pessoaId;
	}

	LocalDate getDataEscuta() {
		return dataEscuta;
	}

	UUID getProfissionalId() {
		return profissionalId;
	}

	String getDinamicaFamiliar() {
		return dinamicaFamiliar;
	}

	String getNecessidadesImediatas() {
		return necessidadesImediatas;
	}

	String getHabilidades() {
		return habilidades;
	}

	String getPotencialidades() {
		return potencialidades;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

	Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
