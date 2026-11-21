package br.org.cesjo.sgi.acesso.interno;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * Tratador de CSRF para SPA, como na documentação do Spring Security: o Angular lê o cookie
 * {@code XSRF-TOKEN} e devolve o valor cru no cabeçalho {@code X-XSRF-TOKEN}; valores vindos de
 * parâmetro usam a proteção contra BREACH. Mesmo comportamento de {@code csrf().spa()}, mas com o
 * repositório de token exposto para o login poder trocá-lo (ADR 0010).
 */
final class TratadorCsrfSpa implements CsrfTokenRequestHandler {

	private final CsrfTokenRequestHandler simples = new CsrfTokenRequestAttributeHandler();

	private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

	@Override
	public void handle(HttpServletRequest requisicao, HttpServletResponse resposta, Supplier<CsrfToken> token) {
		xor.handle(requisicao, resposta, token);
		// Carrega o token já, para o cookie ir na resposta.
		token.get();
	}

	@Override
	public String resolveCsrfTokenValue(HttpServletRequest requisicao, CsrfToken token) {
		String cabecalho = requisicao.getHeader(token.getHeaderName());
		return (StringUtils.hasText(cabecalho) ? simples : xor).resolveCsrfTokenValue(requisicao, token);
	}

}
