package br.org.cesjo.sgi.pessoas.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.pessoas.NovaPessoa;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/** Pessoa com deficiência, candidata ou atendida (tabela {@code pessoa}, ADR 0037). */
@Entity
@Table(name = "pessoa")
class Pessoa {

	@Id
	private UUID id;

	@Column(name = "nome_completo", nullable = false, length = 150)
	private String nomeCompleto;

	@Column(name = "data_nascimento")
	private LocalDate dataNascimento;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_deficiencia", nullable = false, length = 20)
	private TipoDeficiencia tipoDeficiencia;

	@Column(nullable = false, length = 11)
	private String telefone;

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

	protected Pessoa() {
	}

	Pessoa(NovaPessoa nova, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.nomeCompleto = nova.nomeCompleto();
		this.dataNascimento = nova.dataNascimento();
		this.tipoDeficiencia = nova.tipoDeficiencia();
		this.telefone = nova.telefone();
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	/** Aplica os dados novos e devolve os nomes das colunas que mudaram. */
	Set<String> atualizar(NovaPessoa dados, UUID autor, Instant agora) {
		Set<String> campos = new LinkedHashSet<>();
		if (!Objects.equals(nomeCompleto, dados.nomeCompleto())) {
			nomeCompleto = dados.nomeCompleto();
			campos.add("nome_completo");
		}
		if (!Objects.equals(dataNascimento, dados.dataNascimento())) {
			dataNascimento = dados.dataNascimento();
			campos.add("data_nascimento");
		}
		if (tipoDeficiencia != dados.tipoDeficiencia()) {
			tipoDeficiencia = dados.tipoDeficiencia();
			campos.add("tipo_deficiencia");
		}
		if (!Objects.equals(telefone, dados.telefone())) {
			telefone = dados.telefone();
			campos.add("telefone");
		}
		if (!campos.isEmpty()) {
			atualizadoEm = agora;
			atualizadoPor = autor;
		}
		return campos;
	}

	UUID getId() {
		return id;
	}

	String getNomeCompleto() {
		return nomeCompleto;
	}

	LocalDate getDataNascimento() {
		return dataNascimento;
	}

	TipoDeficiencia getTipoDeficiencia() {
		return tipoDeficiencia;
	}

	String getTelefone() {
		return telefone;
	}

}
