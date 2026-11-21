package br.org.cesjo.sgi.acesso.interno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;

/** Gestão de contas de acesso pelo Coordenador (issue #15) e 403 para os demais perfis (issue #14). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, ContasDeTeste.class })
class ContasIntegracaoTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	ContasDeTeste contasDeTeste;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	Auditoria auditoria;

	@Test
	@ComConta(perfil = Perfil.COORDENADOR)
	void criaContaComSenhaTemporariaQueExigeTrocaNoPrimeiroAcesso() throws Exception {
		String login = ContasDeTeste.loginUnico();

		String corpo = mvc
			.perform(post("/api/contas").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(novaConta("  " + login.toUpperCase() + " ", "EQUIPE_TECNICA")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.conta.login").value(login))
			.andExpect(jsonPath("$.conta.perfil").value("EQUIPE_TECNICA"))
			.andExpect(jsonPath("$.conta.ativa").value(true))
			.andExpect(jsonPath("$.conta.trocaSenhaObrigatoria").value(true))
			.andExpect(jsonPath("$.conta.senhaHash").doesNotExist())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String senhaTemporaria = JsonPath.read(corpo, "$.senhaTemporaria");
		String id = JsonPath.read(corpo, "$.conta.id");
		assertThat(senhaTemporaria).hasSize(RegraSenha.TAMANHO_TEMPORARIA);
		verify(auditoria).registrar(
				argThat(evento -> evento.acao() == AcaoAuditoria.CRIAR && id.equals(evento.entidadeId())));

		TestSecurityContextHolder.clearContext();
		mvc.perform(post("/api/sessao/entrar").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"login": "%s", "senha": "%s"}
					""".formatted(login, senhaTemporaria)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trocaSenhaObrigatoria").value(true));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR)
	void recusaLoginRepetidoEInvalido() throws Exception {
		ContaAcesso existente = contasDeTeste.criar(Perfil.CONSULTA);

		mvc.perform(post("/api/contas").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(novaConta(existente.getLogin(), "CONSULTA")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Já existe uma conta com este login."));
		mvc.perform(post("/api/contas").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(novaConta("joão silva", "CONSULTA")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[0].campo").value("login"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR)
	void listaPaginadoOrdenadoPorNome() throws Exception {
		contasDeTeste.criar(Perfil.CONSULTA);
		contasDeTeste.criar(Perfil.CONSULTA);

		mvc.perform(get("/api/contas").param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.page.size").value(1))
			.andExpect(jsonPath("$.content[0].senhaHash").doesNotExist());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR)
	void obtemContaOuResponde404() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);

		mvc.perform(get("/api/contas/{id}", conta.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.login").value(conta.getLogin()));
		mvc.perform(get("/api/contas/{id}", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR)
	void editaNomeEPerfilERegistraOsCamposAlterados() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);

		mvc.perform(put("/api/contas/{id}", conta.getId()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"nome": "Outra Pessoa Fictícia", "perfil": "ADMINISTRATIVO"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value("Outra Pessoa Fictícia"))
			.andExpect(jsonPath("$.perfil").value("ADMINISTRATIVO"));

		verify(auditoria).registrar(argThat(evento -> evento.acao() == AcaoAuditoria.ALTERAR
				&& conta.getId().toString().equals(evento.entidadeId())
				&& evento.camposAlterados().equals(Set.of("nome", "perfil"))));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR)
	void desativaEReativa() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.ADMINISTRATIVO);

		mvc.perform(post("/api/contas/{id}/desativar", conta.getId()).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ativa").value(false));
		mvc.perform(post("/api/contas/{id}/reativar", conta.getId()).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ativa").value(true));

		verify(auditoria, times(2)).registrar(argThat(evento -> evento.acao() == AcaoAuditoria.ALTERAR
				&& conta.getId().toString().equals(evento.entidadeId()) && evento.camposAlterados().equals(Set.of("ativa"))));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR)
	void redefineSenhaExigindoTrocaEDesbloqueando() throws Exception {
		ContaAcesso conta = contasDeTeste.criar(Perfil.CONSULTA);
		jdbc.update("update conta_acesso set bloqueada_ate = now() + interval '1 hour' where id = ?", conta.getId());

		String corpo = mvc.perform(post("/api/contas/{id}/redefinir-senha", conta.getId()).with(csrf()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		String senhaTemporaria = JsonPath.read(corpo, "$.senhaTemporaria");
		ContaAcesso recarregada = contasDeTeste.recarregar(conta.getId());
		assertThat(recarregada.isTrocaSenhaObrigatoria()).isTrue();
		assertThat(recarregada.getBloqueadaAte()).isNull();
		TestSecurityContextHolder.clearContext();
		mvc.perform(post("/api/sessao/entrar").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"login": "%s", "senha": "%s"}
					""".formatted(conta.getLogin(), senhaTemporaria)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trocaSenhaObrigatoria").value(true));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR)
	void ultimaContaAtivaDeCoordenadorNaoPodeSerDesativadaNemPerderOPerfil() throws Exception {
		ContaAcesso ultima = contasDeTeste.criar(Perfil.COORDENADOR);
		jdbc.update("update conta_acesso set ativa = false where perfil = 'COORDENADOR' and id <> ?", ultima.getId());

		mvc.perform(post("/api/contas/{id}/desativar", ultima.getId()).with(csrf()))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("A última conta ativa de Coordenador não pode ser desativada."));
		mvc.perform(put("/api/contas/{id}", ultima.getId()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"nome": "Pessoa Fictícia", "perfil": "CONSULTA"}
					"""))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail")
				.value("A última conta ativa de Coordenador não pode perder o perfil de Coordenador."));

		ContaAcesso outra = contasDeTeste.criar(Perfil.COORDENADOR);
		mvc.perform(post("/api/contas/{id}/desativar", ultima.getId()).with(csrf())).andExpect(status().isOk());
		assertThat(contasDeTeste.recarregar(outra.getId()).isAtiva()).isTrue();
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, id = "00000000-0000-0000-0000-0000000000c1")
	void naoDesativaAPropriaConta() throws Exception {
		UUID propria = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
		contasDeTeste.criar(Perfil.COORDENADOR);
		contasDeTeste.criarComId(propria, Perfil.COORDENADOR);

		mvc.perform(post("/api/contas/{id}/desativar", propria).with(csrf()))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Você não pode desativar a sua própria conta."));
	}

	@Test
	void semSessaoResponde401() throws Exception {
		mvc.perform(get("/api/contas")).andExpect(status().isUnauthorized());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA)
	void equipeTecnicaRecebe403EmTodosOsEndpoints() throws Exception {
		exigir403EmTodosOsEndpoints();
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO)
	void administrativoRecebe403EmTodosOsEndpoints() throws Exception {
		exigir403EmTodosOsEndpoints();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA)
	void consultaRecebe403EmTodosOsEndpoints() throws Exception {
		exigir403EmTodosOsEndpoints();
	}

	private void exigir403EmTodosOsEndpoints() throws Exception {
		UUID id = contasDeTeste.criar(Perfil.CONSULTA).getId();
		List<RequestBuilder> pedidos = List.of(get("/api/contas"), get("/api/contas/{id}", id),
				post("/api/contas").with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(novaConta(ContasDeTeste.loginUnico(), "CONSULTA")),
				put("/api/contas/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
						{"nome": "Pessoa Fictícia", "perfil": "COORDENADOR"}
						"""),
				post("/api/contas/{id}/desativar", id).with(csrf()),
				post("/api/contas/{id}/reativar", id).with(csrf()),
				post("/api/contas/{id}/redefinir-senha", id).with(csrf()));

		for (RequestBuilder pedido : pedidos) {
			mvc.perform(pedido)
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Acesso negado"));
		}
		ContaAcesso alvo = contasDeTeste.recarregar(id);
		assertThat(alvo.isAtiva()).isTrue();
		assertThat(alvo.getPerfil()).isEqualTo(Perfil.CONSULTA);
	}

	private static String novaConta(String login, String perfil) {
		return """
				{"login": "%s", "nome": "Pessoa Fictícia", "perfil": "%s"}
				""".formatted(login, perfil);
	}

}
