package br.org.cesjo.sgi.acesso.interno;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Respostas 401 e 403 da API em {@link ProblemDetail}, em português e sem redirecionar para página de
 * login (ADR 0010).
 */
class RespostasSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

	static final String TITULO_NAO_AUTENTICADO = "Não autenticado";

	static final String TITULO_ACESSO_NEGADO = "Acesso negado";

	private final JsonMapper json;

	RespostasSeguranca(JsonMapper json) {
		this.json = json;
	}

	@Override
	public void commence(HttpServletRequest requisicao, HttpServletResponse resposta, AuthenticationException erro)
			throws IOException {
		naoAutenticado(requisicao, resposta);
	}

	@Override
	public void handle(HttpServletRequest requisicao, HttpServletResponse resposta, AccessDeniedException erro)
			throws IOException {
		String detalhe = erro instanceof CsrfException
				? "O token de segurança da requisição está ausente ou expirou. Recarregue a página e tente de novo."
				: "Sua conta não tem permissão para esta operação.";
		escrever(requisicao, resposta, HttpStatus.FORBIDDEN, TITULO_ACESSO_NEGADO, detalhe);
	}

	void naoAutenticado(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
		escrever(requisicao, resposta, HttpStatus.UNAUTHORIZED, TITULO_NAO_AUTENTICADO,
				"Entre no sistema para continuar. A sessão pode ter expirado por inatividade.");
	}

	void trocaSenhaPendente(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
		escrever(requisicao, resposta, HttpStatus.FORBIDDEN, "Troca de senha obrigatória",
				"Troque a sua senha para continuar usando o sistema.");
	}

	private void escrever(HttpServletRequest requisicao, HttpServletResponse resposta, HttpStatus status,
			String titulo, String detalhe) throws IOException {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("type", "about:blank");
		corpo.put("title", titulo);
		corpo.put("status", status.value());
		corpo.put("detail", detalhe);
		corpo.put("instance", requisicao.getRequestURI());
		resposta.setStatus(status.value());
		resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		json.writeValue(resposta.getOutputStream(), corpo);
	}

}
