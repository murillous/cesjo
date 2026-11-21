package br.org.cesjo.sgi.acesso.interno;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Confere a conta da sessão a cada requisição (ADR 0010):
 *
 * <ul>
 * <li>conta desativada ou removida: encerra a sessão e responde 401;</li>
 * <li>perfil, nome ou exigência de troca de senha mudaram: atualiza o principal da sessão, para que
 * a mudança valha na hora;</li>
 * <li>troca de senha pendente: só deixa passar o necessário para trocar a senha ou sair.</li>
 * </ul>
 *
 * <p>
 * Não é um {@code @Component} para não ser registrado de novo como filtro do servlet.
 */
class FiltroContaVigente extends OncePerRequestFilter {

	/** Caminhos liberados enquanto a conta precisa trocar a senha. */
	private static final Set<String> LIBERADOS_NA_TROCA = Set.of("GET /api/sessao", "GET /api/sessao/csrf",
			"POST /api/sessao/senha", "POST /api/sessao/sair");

	private final ServicoAutenticacao autenticacao;

	private final SecurityContextRepository repositorioContexto;

	private final RespostasSeguranca respostas;

	FiltroContaVigente(ServicoAutenticacao autenticacao, SecurityContextRepository repositorioContexto,
			RespostasSeguranca respostas) {
		this.autenticacao = autenticacao;
		this.repositorioContexto = repositorioContexto;
		this.respostas = respostas;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
			throws ServletException, IOException {
		Authentication atual = SecurityContextHolder.getContext().getAuthentication();
		if (atual == null || !(atual.getPrincipal() instanceof ContaPrincipal principal)) {
			cadeia.doFilter(requisicao, resposta);
			return;
		}
		Optional<ContaPrincipal> vigente = autenticacao.vigente(principal.id());
		if (vigente.isEmpty()) {
			SecurityContextHolder.clearContext();
			HttpSession sessao = requisicao.getSession(false);
			if (sessao != null) {
				sessao.invalidate();
			}
			respostas.naoAutenticado(requisicao, resposta);
			return;
		}
		ContaPrincipal conta = vigente.get();
		if (!conta.equals(principal)) {
			SecurityContext contexto = SecurityContextHolder.createEmptyContext();
			contexto.setAuthentication(conta.autenticacao());
			SecurityContextHolder.setContext(contexto);
			repositorioContexto.saveContext(contexto, requisicao, resposta);
		}
		if (conta.trocaSenhaObrigatoria() && !LIBERADOS_NA_TROCA.contains(requisicao.getMethod() + " " + caminho(requisicao))) {
			respostas.trocaSenhaPendente(requisicao, resposta);
			return;
		}
		cadeia.doFilter(requisicao, resposta);
	}

	private static String caminho(HttpServletRequest requisicao) {
		String caminho = requisicao.getRequestURI().substring(requisicao.getContextPath().length());
		return caminho.length() > 1 && caminho.endsWith("/") ? caminho.substring(0, caminho.length() - 1) : caminho;
	}

}
