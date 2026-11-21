package br.org.cesjo.sgi.acesso.interno;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Confere login e senha, aplica o bloqueio temporário (ADR 0011) e troca a senha da própria conta.
 */
@Service
class ServicoAutenticacao {

	static final String ENTIDADE = "conta_acesso";

	private final ContaAcessoRepositorio contas;

	private final PasswordEncoder codificador;

	private final RegraSenha regraSenha;

	private final Auditoria auditoria;

	private final PropriedadesAcesso propriedades;

	private final Clock relogio = Clock.systemUTC();

	/**
	 * Hash de uma senha qualquer, conferido quando o login não existe, para que a resposta demore o
	 * mesmo que com um login existente e não revele quais logins existem.
	 */
	private final String hashFicticio;

	ServicoAutenticacao(ContaAcessoRepositorio contas, PasswordEncoder codificador, RegraSenha regraSenha,
			Auditoria auditoria, PropriedadesAcesso propriedades) {
		this.contas = contas;
		this.codificador = codificador;
		this.regraSenha = regraSenha;
		this.auditoria = auditoria;
		this.propriedades = propriedades;
		this.hashFicticio = codificador.encode(UUID.randomUUID().toString());
	}

	/**
	 * Devolve a conta quando login e senha conferem e a conta está ativa e sem bloqueio. Qualquer outro
	 * caso devolve vazio, sem dizer o motivo, e é registrado como falha de login.
	 */
	@Transactional
	Optional<ContaPrincipal> autenticar(String loginInformado, String senha) {
		String login = ContaAcesso.normalizarLogin(loginInformado);
		Instant agora = relogio.instant();
		Optional<ContaAcesso> encontrada = login == null || login.isEmpty() ? Optional.empty()
				: contas.findByLogin(login);
		if (encontrada.isEmpty()) {
			codificador.matches(senha, hashFicticio);
			return falha(login);
		}
		ContaAcesso conta = encontrada.get();
		boolean senhaConfere = codificador.matches(senha, conta.getSenhaHash());
		if (!conta.isAtiva() || conta.bloqueada(agora)) {
			return falha(login);
		}
		if (!senhaConfere) {
			conta.registrarFalha(propriedades.tentativasMaximas(), propriedades.duracaoBloqueio(), agora);
			return falha(login);
		}
		conta.registrarSucesso();
		auditoria.registrar(EventoAuditoria.login(conta.getId(), conta.getLogin()));
		return Optional.of(ContaPrincipal.de(conta));
	}

	private Optional<ContaPrincipal> falha(String login) {
		auditoria.registrar(EventoAuditoria.loginFalho(login));
		return Optional.empty();
	}

	/** Retrato atual da conta, ou vazio se ela não existe mais ou foi desativada. */
	@Transactional(readOnly = true)
	Optional<ContaPrincipal> vigente(UUID id) {
		return contas.findById(id).filter(ContaAcesso::isAtiva).map(ContaPrincipal::de);
	}

	/** A conta logada troca a própria senha. Encerra a exigência de troca. */
	@Transactional
	ContaPrincipal trocarSenha(UUID id, String senhaAtual, String novaSenha) {
		ContaAcesso conta = contas.findById(id)
			.filter(ContaAcesso::isAtiva)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
		if (!codificador.matches(senhaAtual, conta.getSenhaHash())) {
			throw new RegraNegocioException("A senha atual está incorreta.");
		}
		regraSenha.validar(novaSenha, conta.getLogin());
		if (codificador.matches(novaSenha, conta.getSenhaHash())) {
			throw new RegraNegocioException("A nova senha precisa ser diferente da atual.");
		}
		conta.trocarSenha(codificador.encode(novaSenha), relogio.instant());
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, conta.getId(),
				Set.of("senha_hash", "troca_senha_obrigatoria")));
		return ContaPrincipal.de(conta);
	}

}
