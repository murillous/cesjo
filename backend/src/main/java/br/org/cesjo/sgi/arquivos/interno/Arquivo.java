package br.org.cesjo.sgi.arquivos.interno;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import br.org.cesjo.sgi.arquivos.MetadadosArquivo;
import br.org.cesjo.sgi.arquivos.TipoArquivo;
import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;

/** Metadados de um arquivo guardado (tabela {@code arquivo}, ADR 0026). Imutável depois de gravado. */
@Entity
@Table(name = "arquivo")
class Arquivo implements Persistable<UUID> {

	@Id
	private UUID id;

	/** Cifrado: o nome de um laudo pode identificar a pessoa. */
	@Column(name = "nome_original", nullable = false)
	@Convert(converter = TextoCifradoConverter.class)
	private String nomeOriginal;

	@Column(name = "tipo_mime", nullable = false, length = 50)
	private String tipoMime;

	@Column(nullable = false)
	private long tamanho;

	@Column(nullable = false, length = 64)
	private String sha256;

	@Column(name = "caminho_armazenamento", nullable = false, length = 255)
	private String caminhoArmazenamento;

	@Column(name = "chave_id", nullable = false, length = 32)
	private String chaveId;

	@Column(name = "enviado_por")
	private UUID enviadoPor;

	@Column(name = "enviado_em", nullable = false)
	private Instant enviadoEm;

	@Transient
	private boolean novo;

	protected Arquivo() {
	}

	Arquivo(UUID id, String nomeOriginal, TipoArquivo tipo, long tamanho, String sha256, String caminhoArmazenamento,
			String chaveId, UUID enviadoPor, Instant enviadoEm) {
		this.id = id;
		this.nomeOriginal = nomeOriginal;
		this.tipoMime = tipo.tipoMime();
		this.tamanho = tamanho;
		this.sha256 = sha256;
		this.caminhoArmazenamento = caminhoArmazenamento;
		this.chaveId = chaveId;
		this.enviadoPor = enviadoPor;
		this.enviadoEm = enviadoEm;
		this.novo = true;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return novo;
	}

	String caminhoArmazenamento() {
		return caminhoArmazenamento;
	}

	MetadadosArquivo metadados() {
		return new MetadadosArquivo(id, nomeOriginal, TipoArquivo.doTipoMime(tipoMime), tamanho, sha256, enviadoPor,
				enviadoEm);
	}

}
