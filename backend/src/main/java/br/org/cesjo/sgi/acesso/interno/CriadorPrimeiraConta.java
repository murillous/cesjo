package br.org.cesjo.sgi.acesso.interno;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;

/**
 * Na subida, cria a primeira conta de Coordenador a partir das variáveis {@code SGI_COORDENADOR_*}
 * quando não existe nenhuma conta (ADR 0014). A senha informada é temporária: a conta precisa
 * trocá-la no primeiro acesso.
 */
@Component
class CriadorPrimeiraConta implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CriadorPrimeiraConta.class);

	private static final String LOGIN_VALIDO = "[a-z0-9._-]{3,60}";

	private final ContaAcessoRepositorio contas;

	private final PasswordEncoder codificador;

	private final RegraSenha regraSenha;

	private final Auditoria auditoria;

	private final PropriedadesAcesso.PrimeiraConta primeiraConta;

	CriadorPrimeiraConta(ContaAcessoRepositorio contas, PasswordEncoder codificador, RegraSenha regraSenha,
			Auditoria auditoria, PropriedadesAcesso propriedades) {
		this.contas = contas;
		this.codificador = codificador;
		this.regraSenha = regraSenha;
		this.auditoria = auditoria;
		this.primeiraConta = propriedades.primeiraConta();
	}

	@Override
	@Transactional
	public void run(ApplicationArguments argumentos) {
		if (contas.count() > 0) {
			return;
		}
		if (!primeiraConta.informada()) {
			log.warn("Nenhuma conta de acesso cadastrada. Defina SGI_COORDENADOR_LOGIN e SGI_COORDENADOR_SENHA "
					+ "para criar a primeira conta de Coordenador.");
			return;
		}
		String login = ContaAcesso.normalizarLogin(primeiraConta.login());
		if (!login.matches(LOGIN_VALIDO)) {
			throw new IllegalStateException("SGI_COORDENADOR_LOGIN inválido: use de 3 a 60 caracteres entre letras "
					+ "sem acento, números, ponto, hífen e sublinhado.");
		}
		regraSenha.validar(primeiraConta.senha(), login);
		String nome = primeiraConta.nome() == null || primeiraConta.nome().isBlank() ? "Coordenador"
				: primeiraConta.nome();
		ContaAcesso conta = new ContaAcesso(login, nome, Perfil.COORDENADOR, codificador.encode(primeiraConta.senha()),
				null, Instant.now());
		contas.save(conta);
		auditoria.registrar(EventoAuditoria.criacao(ServicoAutenticacao.ENTIDADE, conta.getId()));
		log.info("Primeira conta de Coordenador criada com o login {}.", login);
	}

}
