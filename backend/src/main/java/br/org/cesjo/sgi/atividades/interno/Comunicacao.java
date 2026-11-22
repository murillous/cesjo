package br.org.cesjo.sgi.atividades.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.atividades.DirecaoComunicacao;
import br.org.cesjo.sgi.atividades.TipoComunicacao;
import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;

/**
 * Contato ou comunicação do RF011 (tabela {@code comunicacao}, ADR 0160). Contraparte e assunto ficam em
 * claro para a listagem; a descrição, opcional, é cifrada na aplicação. Ofícios, declarações e
 * encaminhamentos têm direção e podem ter um anexo, guardado no módulo Arquivos (ADR 0164).
 */
@Entity
@Table(name = "comunicacao")
class Comunicacao {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private TipoComunicacao tipo;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private DirecaoComunicacao direcao;

	@Column(name = "data_comunicacao", nullable = false)
	private LocalDate data;

	@Column(nullable = false, length = 200)
	private String contraparte;

	@Column(nullable = false, length = 200)
	private String assunto;

	@Convert(converter = TextoCifradoConverter.class)
	@Column(columnDefinition = "text")
	private String descricao;

	/** Anexo opcional, pelo {@code id} do módulo Arquivos. */
	@Column(name = "arquivo_id")
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

	protected Comunicacao() {
	}

	Comunicacao(DadosComunicacao dados, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		aplicar(dados);
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	/** Aplica a correção e devolve os nomes das colunas que mudaram. */
	Set<String> corrigir(DadosComunicacao dados, UUID autor, Instant agora) {
		Set<String> alterados = new LinkedHashSet<>();
		if (tipo != dados.tipo()) {
			alterados.add("tipo");
		}
		if (direcao != dados.direcao()) {
			alterados.add("direcao");
		}
		if (!data.equals(dados.data())) {
			alterados.add("data_comunicacao");
		}
		if (!contraparte.equals(dados.contraparte().strip())) {
			alterados.add("contraparte");
		}
		if (!assunto.equals(dados.assunto().strip())) {
			alterados.add("assunto");
		}
		if (!Objects.equals(descricao, dados.descricaoLimpa())) {
			alterados.add("descricao");
		}
		if (!alterados.isEmpty()) {
			aplicar(dados);
			this.atualizadoEm = agora;
			this.atualizadoPor = autor;
		}
		return alterados;
	}

	private void aplicar(DadosComunicacao dados) {
		this.tipo = dados.tipo();
		this.direcao = dados.direcao();
		this.data = dados.data();
		this.contraparte = dados.contraparte().strip();
		this.assunto = dados.assunto().strip();
		this.descricao = dados.descricaoLimpa();
	}

	/** Liga o anexo, no lugar do anterior se houver. */
	void anexar(UUID arquivoId, UUID autor, Instant agora) {
		this.arquivoId = arquivoId;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	/** Desliga o anexo. Devolve {@code false} quando não havia anexo. */
	boolean removerAnexo(UUID autor, Instant agora) {
		if (arquivoId == null) {
			return false;
		}
		anexar(null, autor, agora);
		return true;
	}

	UUID getId() {
		return id;
	}

	TipoComunicacao getTipo() {
		return tipo;
	}

	DirecaoComunicacao getDirecao() {
		return direcao;
	}

	UUID getArquivoId() {
		return arquivoId;
	}

	LocalDate getData() {
		return data;
	}

	String getContraparte() {
		return contraparte;
	}

	String getAssunto() {
		return assunto;
	}

	String getDescricao() {
		return descricao;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

	Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
