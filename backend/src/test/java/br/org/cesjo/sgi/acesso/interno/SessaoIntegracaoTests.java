package br.org.cesjo.sgi.acesso.interno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;

/** Login por sessão, CSRF, logout, bloqueio e troca de senha (issue #13). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, ContasDeTeste.class })
class SessaoIntegracaoTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	ContasDeTeste contasDeTeste;

	@MockitoSpyBean
	Auditoria auditoria;

	@Test
	void semSessaoResponde401EmProblemDetail() throws Exception {
		mvc.perform(get("/api/sessao"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.title").value("Não autenticado"));
	}

	@Test
	void loginSemTokenCsrfResponde403EmProblemDetail() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);

		mvc.perform(post("/api/sessao/entrar").contentType(MediaType.APPLICATION_JSON)
			.content(credenciais(conta.getLogin(), ContasDeTeste.SENHA)))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Acesso negado"));
	}

	@Test
	void fluxoEntrarVerSessaoESair() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.COORDENADOR);

		MvcResult login = mvc
			.perform(post("/api/sessao/entrar").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(credenciais(conta.getLogin().toUpperCase(), ContasDeTeste.SENHA)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(conta.getId().toString()))
			.andExpect(jsonPath("$.login").value(conta.getLogin()))
			.andExpect(jsonPath("$.perfil").value("COORDENADOR"))
			.andExpect(jsonPath("$.permissoes").isArray())
			.andExpect(jsonPath("$.trocaSenhaObrigatoria").value(false))
			.andReturn();
		MockHttpSession sessao = (MockHttpSession) login.getRequest().getSession(false);
		assertThat(sessao).isNotNull();

		mvc.perform(get("/api/sessao").session(sessao))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value("Pessoa Fictícia"))
			.andExpect(jsonPath("$.permissoes[?(@ == 'CONTAS_GERENCIAR')]").exists());

		mvc.perform(post("/api/sessao/sair").session(sessao).with(csrf())).andExpect(status().isNoContent());

		mvc.perform(get("/api/sessao").session(sessao)).andExpect(status().isUnauthorized());
		verify(auditoria).registrar(argThat(evento -> evento.acao() == AcaoAuditoria.LOGIN
				&& conta.getLogin().equals(evento.login()) && conta.getId().equals(evento.contaId())));
		verify(auditoria).registrar(argThat(evento -> evento.acao() == AcaoAuditoria.LOGOUT
				&& conta.getId().equals(evento.contaId())));
	}

	@Test
	void senhaGuardadaComArgon2() {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);

		assertThat(contasDeTeste.recarregar(conta.getId()).getSenhaHash()).startsWith("$argon2id$");
	}

	@Test
	void senhaErradaResponde401SemDizerOMotivoERegistraFalha() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);

		entrar(conta.getLogin(), "senha-errada-qualquer").andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Não foi possível entrar"));
		entrar("login-que-nao-existe", "qualquer").andExpect(status().isUnauthorized());

		verify(auditoria, atLeastOnce()).registrar(argThat(
				evento -> evento.acao() == AcaoAuditoria.LOGIN_FALHO && conta.getLogin().equals(evento.login())));
		assertThat(contasDeTeste.recarregar(conta.getId()).getTentativasFalhas()).isEqualTo(1);
	}

	@Test
	void bloqueiaTemporariamenteAposTentativasErradas() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);
		for (int i = 0; i < 5; i++) {
			entrar(conta.getLogin(), "senha-errada-qualquer").andExpect(status().isUnauthorized());
		}

		entrar(conta.getLogin(), ContasDeTeste.SENHA).andExpect(status().isUnauthorized());
		assertThat(contasDeTeste.recarregar(conta.getId()).getBloqueadaAte()).isNotNull();
	}

	@Test
	void acertoZeraAsTentativasErradas() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);
		entrar(conta.getLogin(), "senha-errada-qualquer").andExpect(status().isUnauthorized());

		entrar(conta.getLogin(), ContasDeTeste.SENHA).andExpect(status().isOk());
		assertThat(contasDeTeste.recarregar(conta.getId()).getTentativasFalhas()).isZero();
	}

	@Test
	void contaDesativadaNaoEntra() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);
		contasDeTeste.desativar(conta.getId());

		entrar(conta.getLogin(), ContasDeTeste.SENHA).andExpect(status().isUnauthorized());
	}

	@Test
	void desativarContaEncerraSessaoAberta() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);
		MockHttpSession sessao = sessao(conta.getLogin(), ContasDeTeste.SENHA);

		contasDeTeste.desativar(conta.getId());

		mvc.perform(get("/api/sessao").session(sessao)).andExpect(status().isUnauthorized());
	}

	@Test
	void trocaObrigatoriaBloqueiaOResteAteTrocarASenha() throws Exception {
		ContaAcesso conta = contasDeTeste.criarComTrocaPendente(Perfil.COORDENADOR);
		MockHttpSession sessao = sessao(conta.getLogin(), ContasDeTeste.SENHA);

		mvc.perform(get("/api/sessao").session(sessao))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trocaSenhaObrigatoria").value(true));
		mvc.perform(get("/api/contas").session(sessao))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.title").value("Troca de senha obrigatória"));

		mvc.perform(post("/api/sessao/senha").session(sessao)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(troca("senha-atual-errada", "uma-senha-nova-longa")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("A senha atual está incorreta."));
		mvc.perform(post("/api/sessao/senha").session(sessao)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(troca(ContasDeTeste.SENHA, "curta")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("A senha precisa ter pelo menos 12 caracteres."));
		mvc.perform(post("/api/sessao/senha").session(sessao)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(troca(ContasDeTeste.SENHA, "uma-senha-nova-longa")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trocaSenhaObrigatoria").value(false));

		mvc.perform(get("/api/contas").session(sessao)).andExpect(status().isOk());
		entrar(conta.getLogin(), "uma-senha-nova-longa").andExpect(status().isOk())
			.andExpect(jsonPath("$.trocaSenhaObrigatoria").value(false));
	}

	@Test
	void mudancaDePerfilValeNaSessaoAberta() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.COORDENADOR);
		MockHttpSession sessao = sessao(conta.getLogin(), ContasDeTeste.SENHA);
		mvc.perform(get("/api/contas").session(sessao)).andExpect(status().isOk());

		contasDeTeste.alterarPerfil(conta.getId(), Perfil.CONSULTA);

		mvc.perform(get("/api/contas").session(sessao)).andExpect(status().isForbidden());
		mvc.perform(get("/api/sessao").session(sessao)).andExpect(jsonPath("$.perfil").value("CONSULTA"));
	}

	private org.springframework.test.web.servlet.ResultActions entrar(String login, String senha) throws Exception {
		return mvc.perform(post("/api/sessao/entrar").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(credenciais(login, senha)));
	}

	private MockHttpSession sessao(String login, String senha) throws Exception {
		return (MockHttpSession) entrar(login, senha).andExpect(status().isOk())
			.andReturn()
			.getRequest()
			.getSession(false);
	}

	private static String credenciais(String login, String senha) {
		return """
				{"login": "%s", "senha": "%s"}
				""".formatted(login, senha);
	}

	private static String troca(String atual, String nova) {
		return """
				{"senhaAtual": "%s", "novaSenha": "%s"}
				""".formatted(atual, nova);
	}

}
