package br.org.cesjo.sgi.notificacoes.interno;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.notificacoes.NovaNotificacao;

/**
 * Notificação interna destinada a um perfil ou a uma conta (ADR 0030). É imutável depois de criada; o
 * estado de lida de cada conta fica em {@link LeituraNotificacao}.
 */
@Entity
@Table(name = "notificacao")
class Notificacao {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "perfil_destino")
	private Perfil perfilDestino;

	@Column(name = "conta_destino")
	private UUID contaDestino;

	private String tipo;

	private String mensagem;

	private String referencia;

	@Column(name = "criada_em")
	private Instant criadaEm;

	@Column(name = "criado_por")
	private UUID criadoPor;

	@Column(name = "atualizado_em")
	private Instant atualizadoEm;

	@Column(name = "atualizado_por")
	private UUID atualizadoPor;

	protected Notificacao() {
	}

	private Notificacao(Perfil perfilDestino, UUID contaDestino, NovaNotificacao conteudo, UUID autor) {
		this.id = UUID.randomUUID();
		this.perfilDestino = perfilDestino;
		this.contaDestino = contaDestino;
		this.tipo = conteudo.tipo();
		this.mensagem = conteudo.mensagem();
		this.referencia = conteudo.referencia();
		this.criadaEm = Instant.now();
		this.criadoPor = autor;
		this.atualizadoEm = this.criadaEm;
		this.atualizadoPor = autor;
	}

	static Notificacao paraPerfil(Perfil perfil, NovaNotificacao conteudo, UUID autor) {
		return new Notificacao(perfil, null, conteudo, autor);
	}

	static Notificacao paraConta(UUID contaId, NovaNotificacao conteudo, UUID autor) {
		return new Notificacao(null, contaId, conteudo, autor);
	}

	UUID id() {
		return id;
	}

}
