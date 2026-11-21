package br.org.cesjo.sgi.pessoas.interno;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.pessoas.TipoDocumento;

/**
 * Documento anexado a uma pessoa (tabela {@code documento_pessoa}, ADR 0055). Um por tipo; trocar o
 * arquivo atualiza o registro, e o arquivo antigo continua guardado no módulo Arquivos.
 */
@Entity
@Table(name = "documento_pessoa")
class Documento {

	@Id
	private UUID id;

	@Column(name = "pessoa_id", nullable = false, updatable = false)
	private UUID pessoaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30, updatable = false)
	private TipoDocumento tipo;

	@Column(name = "arquivo_id", nullable = false)
	private UUID arquivoId;

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

	protected Documento() {
	}

	Documento(UUID pessoaId, TipoDocumento tipo, UUID arquivoId, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.pessoaId = pessoaId;
		this.tipo = tipo;
		this.arquivoId = arquivoId;
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	void trocarArquivo(UUID arquivoId, UUID autor, Instant agora) {
		this.arquivoId = arquivoId;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	UUID getPessoaId() {
		return pessoaId;
	}

	TipoDocumento getTipo() {
		return tipo;
	}

	UUID getArquivoId() {
		return arquivoId;
	}

	Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
