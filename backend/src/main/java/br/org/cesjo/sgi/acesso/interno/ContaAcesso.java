package br.org.cesjo.sgi.acesso.interno;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.acesso.Perfil;

/**
 * Conta de acesso ao sistema (seção 4.1 do documento de requisitos). A senha só existe como hash
 * Argon2 (ADR 0003).
 */
@Entity
@Table(name = "conta_acesso")
class ContaAcesso {

	@Id
	private UUID id;

	@Column(nullable = false, length = 60)
	private String login;

	@Column(nullable = false, length = 150)
	private String nome;

	@Column(name = "senha_hash", nullable = false)
	private String senhaHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Perfil perfil;

	@Column(nullable = false)
	private boolean ativa;

	@Column(name = "troca_senha_obrigatoria", nullable = false)
	private boolean trocaSenhaObrigatoria;

	@Column(name = "tentativas_falhas", nullable = false)
	private int tentativasFalhas;

	@Column(name = "bloqueada_ate")
	private Instant bloqueadaAte;

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

	protected ContaAcesso() {
	}

	/**
	 * Conta nova, ativa, que precisa trocar a senha no primeiro acesso.
	 */
	ContaAcesso(String login, String nome, Perfil perfil, String senhaHash, UUID criadoPor, Instant agora) {
		this.id = UUID.randomUUID();
		this.login = normalizarLogin(login);
		this.nome = nome.strip();
		this.perfil = perfil;
		this.senhaHash = senhaHash;
		this.ativa = true;
		this.trocaSenhaObrigatoria = true;
		this.criadoEm = agora;
		this.criadoPor = criadoPor;
		this.atualizadoEm = agora;
		this.atualizadoPor = criadoPor;
	}

	/** Logins são guardados sem espaços nas pontas e em minúsculas. */
	static String normalizarLogin(String login) {
		return login == null ? null : login.strip().toLowerCase(java.util.Locale.ROOT);
	}

	boolean bloqueada(Instant agora) {
		return bloqueadaAte != null && bloqueadaAte.isAfter(agora);
	}

	/**
	 * Conta uma tentativa falha. Ao chegar ao máximo, bloqueia a conta pela duração dada e zera o
	 * contador.
	 */
	void registrarFalha(int tentativasMaximas, Duration duracaoBloqueio, Instant agora) {
		tentativasFalhas++;
		if (tentativasFalhas >= tentativasMaximas) {
			bloqueadaAte = agora.plus(duracaoBloqueio);
			tentativasFalhas = 0;
		}
	}

	void registrarSucesso() {
		tentativasFalhas = 0;
		bloqueadaAte = null;
	}

	/** Senha escolhida pela própria conta: deixa de exigir troca. */
	void trocarSenha(String novoHash, Instant agora) {
		senhaHash = novoHash;
		trocaSenhaObrigatoria = false;
		tocar(id, agora);
	}

	/** Senha temporária definida pelo Coordenador: exige troca e desfaz o bloqueio. */
	void redefinirSenha(String novoHash, UUID responsavel, Instant agora) {
		senhaHash = novoHash;
		trocaSenhaObrigatoria = true;
		registrarSucesso();
		tocar(responsavel, agora);
	}

	void alterarNome(String novoNome, UUID responsavel, Instant agora) {
		nome = novoNome.strip();
		tocar(responsavel, agora);
	}

	void alterarPerfil(Perfil novoPerfil, UUID responsavel, Instant agora) {
		perfil = novoPerfil;
		tocar(responsavel, agora);
	}

	void desativar(UUID responsavel, Instant agora) {
		ativa = false;
		tocar(responsavel, agora);
	}

	void reativar(UUID responsavel, Instant agora) {
		ativa = true;
		registrarSucesso();
		tocar(responsavel, agora);
	}

	private void tocar(UUID responsavel, Instant agora) {
		atualizadoEm = agora;
		atualizadoPor = responsavel;
	}

	UUID getId() {
		return id;
	}

	String getLogin() {
		return login;
	}

	String getNome() {
		return nome;
	}

	String getSenhaHash() {
		return senhaHash;
	}

	Perfil getPerfil() {
		return perfil;
	}

	boolean isAtiva() {
		return ativa;
	}

	boolean isTrocaSenhaObrigatoria() {
		return trocaSenhaObrigatoria;
	}

	int getTentativasFalhas() {
		return tentativasFalhas;
	}

	Instant getBloqueadaAte() {
		return bloqueadaAte;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

	Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
