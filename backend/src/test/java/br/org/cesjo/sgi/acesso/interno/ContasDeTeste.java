package br.org.cesjo.sgi.acesso.interno;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.acesso.Perfil;

/**
 * Cria contas com dados fictícios e logins únicos. O Postgres dos testes é compartilhado, então nenhum
 * teste conta com tabela vazia (ADR 0008).
 */
@TestComponent
class ContasDeTeste {

	static final String SENHA = "senha-de-teste-longa";

	private final ContaAcessoRepositorio contas;

	private final PasswordEncoder codificador;

	private final JdbcTemplate jdbc;

	ContasDeTeste(ContaAcessoRepositorio contas, PasswordEncoder codificador, JdbcTemplate jdbc) {
		this.contas = contas;
		this.codificador = codificador;
		this.jdbc = jdbc;
	}

	static String loginUnico() {
		return "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
	}

	/** Conta ativa, com a senha {@link #SENHA} já trocada. */
	@Transactional
	ContaAcesso criar(Perfil perfil) {
		ContaAcesso conta = new ContaAcesso(loginUnico(), "Pessoa Fictícia", perfil, codificador.encode(SENHA), null,
				Instant.now());
		conta.trocarSenha(codificador.encode(SENHA), Instant.now());
		return contas.save(conta);
	}

	/** Conta ativa que ainda precisa trocar a senha {@link #SENHA}. */
	@Transactional
	ContaAcesso criarComTrocaPendente(Perfil perfil) {
		return contas.save(new ContaAcesso(loginUnico(), "Pessoa Fictícia", perfil, codificador.encode(SENHA), null,
				Instant.now()));
	}

	/** Conta ativa com id fixo, para testes que precisam que ela seja a conta da requisição. */
	ContaAcesso criarComId(UUID id, Perfil perfil) {
		jdbc.update("delete from conta_acesso where id = ?", id);
		UUID gerado = criar(perfil).getId();
		jdbc.update("update conta_acesso set id = ? where id = ?", id, gerado);
		return recarregar(id);
	}

	@Transactional
	void desativar(UUID id) {
		contas.findById(id).orElseThrow().desativar(null, Instant.now());
	}

	@Transactional
	void alterarPerfil(UUID id, Perfil perfil) {
		contas.findById(id).orElseThrow().alterarPerfil(perfil, null, Instant.now());
	}

	@Transactional(readOnly = true)
	ContaAcesso recarregar(UUID id) {
		return contas.findById(id).orElseThrow();
	}

}
