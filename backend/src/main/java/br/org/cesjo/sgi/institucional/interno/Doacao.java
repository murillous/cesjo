package br.org.cesjo.sgi.institucional.interno;

import java.math.BigDecimal;
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

import br.org.cesjo.sgi.institucional.DirecaoDoacao;
import br.org.cesjo.sgi.institucional.TipoDoacao;

/** Doação recebida ou realizada pelo Centro-Dia (tabela {@code doacao}, RF015, ADR 0178). */
@Entity
@Table(name = "doacao")
class Doacao {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DirecaoDoacao direcao;

	@Column(nullable = false)
	private LocalDate data;

	/** Quem doou ao Centro-Dia ou quem recebeu dele: pessoa, empresa ou instituição. */
	@Column(nullable = false, length = 200)
	private String contraparte;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TipoDoacao tipo;

	@Column(nullable = false, length = 2000)
	private String descricao;

	@Column(length = 100)
	private String quantidade;

	/** Em reais, só para doação em dinheiro. */
	@Column(precision = 12, scale = 2)
	private BigDecimal valor;

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

	protected Doacao() {
	}

	Doacao(DadosDoacao dados, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.criadoEm = agora;
		this.criadoPor = autor;
		aplicar(dados, autor, agora);
	}

	void aplicar(DadosDoacao dados, UUID autor, Instant agora) {
		this.direcao = dados.direcao();
		this.data = dados.data();
		this.contraparte = dados.contraparte().strip();
		this.tipo = dados.tipo();
		this.descricao = dados.descricao().strip();
		this.quantidade = DadosDoacao.texto(dados.quantidade());
		this.valor = dados.valorNormalizado();
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	DirecaoDoacao getDirecao() {
		return direcao;
	}

	LocalDate getData() {
		return data;
	}

	String getContraparte() {
		return contraparte;
	}

	TipoDoacao getTipo() {
		return tipo;
	}

	String getDescricao() {
		return descricao;
	}

	String getQuantidade() {
		return quantidade;
	}

	BigDecimal getValor() {
		return valor;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

	Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
