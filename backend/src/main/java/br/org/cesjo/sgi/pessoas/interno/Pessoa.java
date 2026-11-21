package br.org.cesjo.sgi.pessoas.interno;

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
