package br.org.cesjo.sgi.acesso.interno;

import java.util.Arrays;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.acesso.ContaAtual;
import br.org.cesjo.sgi.acesso.ContaAutenticada;
import br.org.cesjo.sgi.acesso.Permissao;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;

/**
 * Sessão da conta logada: entrar, ver a sessão, trocar a senha e obter o token CSRF. Sair é
 * {@code POST /api/sessao/sair}, tratado pelo filtro de logout do Spring Security.
 */
@RestController
@RequestMapping("/api/sessao")
class SessaoControlador {

	private final ProvedorAutenticacaoContas provedor;

	private final ServicoAutenticacao autenticacao;

	private final SecurityContextRepository repositorioContexto;

	private final CookieCsrfTokenRepository repositorioCsrf;

	private final ContaAtual contaAtual;

	private final PropriedadesAcesso propriedades;

	SessaoControlador(ProvedorAutenticacaoContas provedor, ServicoAutenticacao autenticacao,
			SecurityContextRepository repositorioContexto, CookieCsrfTokenRepository repositorioCsrf,
			ContaAtual contaAtual, PropriedadesAcesso propriedades) {
		this.provedor = provedor;
		this.autenticacao = autenticacao;
		this.repositorioContexto = repositorioContexto;
		this.repositorioCsrf = repositorioCsrf;
		this.contaAtual = contaAtual;
		this.propriedades = propriedades;
	}

	/** Entrega o token CSRF (e grava o cookie {@code XSRF-TOKEN}), antes do login. */
	@GetMapping("/csrf")
	Map<String, String> csrf(CsrfToken token) {
		return Map.of("cabecalho", token.getHeaderName(), "token", token.getToken());
	}

	@PostMapping("/entrar")
	ResponseEntity<?> entrar(@Valid @RequestBody Credenciais credenciais, HttpServletRequest requisicao,
			HttpServletResponse resposta) {
		Authentication autenticada;
		try {
			autenticada = provedor
				.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(credenciais.login(), credenciais.senha()));
		}
		catch (AuthenticationException erro) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(credenciaisInvalidas());
		}
		// Sessão nova a cada login, contra fixação de sessão.
		HttpSession anterior = requisicao.getSession(false);
		if (anterior != null) {
			anterior.invalidate();
		}
		requisicao.getSession(true);
		guardar(autenticada, requisicao, resposta);
		// Token CSRF novo para a sessão autenticada.
		repositorioCsrf.saveToken(repositorioCsrf.generateToken(requisicao), requisicao, resposta);
		return ResponseEntity.ok(SessaoResposta.de((ContaPrincipal) autenticada.getPrincipal()));
	}

	@GetMapping
	SessaoResposta sessao(Authentication autenticada) {
		if (autenticada.getPrincipal() instanceof ContaPrincipal conta) {
			return SessaoResposta.de(conta);
		}
		// Principal de outro tipo (contas de teste): mesmo formato, permissões pelas authorities.
		ContaAutenticada conta = contaAtual.obter()
			.orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
		return new SessaoResposta(conta.id(), conta.login(), conta.nome(), conta.perfil(),
				Arrays.stream(Permissao.values())
					.filter(permissao -> autenticada.getAuthorities()
						.stream()
						.anyMatch(authority -> permissao.name().equals(authority.getAuthority())))
					.toList(),
				false);
	}

	@PostMapping("/senha")
	SessaoResposta trocarSenha(@Valid @RequestBody TrocaSenha troca, HttpServletRequest requisicao,
			HttpServletResponse resposta) {
		ContaAutenticada conta = contaAtual.obter()
			.orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
		ContaPrincipal atualizada = autenticacao.trocarSenha(conta.id(), troca.senhaAtual(), troca.novaSenha());
		guardar(atualizada.autenticacao(), requisicao, resposta);
		return SessaoResposta.de(atualizada);
	}

	private void guardar(Authentication autenticada, HttpServletRequest requisicao, HttpServletResponse resposta) {
		SecurityContext contexto = SecurityContextHolder.createEmptyContext();
		contexto.setAuthentication(autenticada);
		SecurityContextHolder.setContext(contexto);
		repositorioContexto.saveContext(contexto, requisicao, resposta);
	}

	private ProblemDetail credenciaisInvalidas() {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
				"Login ou senha incorretos. Depois de " + propriedades.tentativasMaximas()
						+ " tentativas erradas seguidas, a conta fica bloqueada por "
						+ propriedades.duracaoBloqueio().toMinutes() + " minutos.");
		problema.setTitle("Não foi possível entrar");
		return problema;
	}

}
