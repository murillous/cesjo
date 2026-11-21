package br.org.cesjo.sgi.acesso.interno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.Perfil;

/**
 * CSRF de verdade, como o Angular usa: cookie {@code XSRF-TOKEN} e cabeçalho {@code X-XSRF-TOKEN}.
 *
 * <p>
 * Fica num contexto próprio (a propriedade abaixo só muda a chave do cache de contextos) e nunca usa
 * {@code csrf()} do Spring Security Test, que troca o repositório de token do filtro de CSRF do
 * contexto pelo de sessão.
 */
@SpringBootTest(properties = "sgi.teste.contexto=csrf-real")
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, ContasDeTeste.class })
class CsrfIntegracaoTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	ContasDeTeste contasDeTeste;

	@Test
	void entregaTokenCsrfEmCookieLegivelPeloAngular() throws Exception {
		MvcResult resultado = mvc.perform(get("/api/sessao/csrf"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cabecalho").value("X-XSRF-TOKEN"))
			.andReturn();

		Cookie cookie = resultado.getResponse().getCookie("XSRF-TOKEN");
		assertThat(cookie).isNotNull();
		assertThat(cookie.isHttpOnly()).isFalse();
		assertThat(cookie.getValue()).isNotBlank();
	}

	@Test
	void entraComTokenDoCookieNoCabecalhoComoOAngular() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);
		Cookie xsrf = mvc.perform(get("/api/sessao/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");

		mvc.perform(post("/api/sessao/entrar").cookie(xsrf)
			.header("X-XSRF-TOKEN", xsrf.getValue())
			.contentType(MediaType.APPLICATION_JSON)
			.content(credenciais(conta.getLogin(), ContasDeTeste.SENHA)))
			.andExpect(status().isOk());
	}

	private static String credenciais(String login, String senha) {
		return """
				{"login": "%s", "senha": "%s"}
				""".formatted(login, senha);
	}

}
