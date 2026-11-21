package br.org.cesjo.sgi.notificacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ContaAutenticada;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.acesso.Permissao;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;

/**
 * Fluxo principal das notificações internas (issue #19): criação pela API Java, caixa da conta logada
 * pela API HTTP, isolamento entre contas e perfis e registro de auditoria.
 *
 * <p>
 * O Postgres de teste é compartilhado, então cada teste usa contas novas e mensagens únicas e nunca
 * conta com tabela vazia: notificações de perfil criadas por outros testes podem aparecer.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class NotificacoesIntegracaoTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	Notificacoes notificacoes;

	@MockitoSpyBean
	Auditoria auditoria;

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	void notificacaoDePerfilChegaATodasAsContasDoPerfilECadaUmaMarcaASua() throws Exception {
		RequestPostProcessor ana = novaConta(Perfil.EQUIPE_TECNICA);
		RequestPostProcessor bruno = novaConta(Perfil.EQUIPE_TECNICA);
		RequestPostProcessor outroPerfil = novaConta(Perfil.ADMINISTRATIVO);
		long naoLidasAntesAna = contarNaoLidas(ana);
		long naoLidasAntesBruno = contarNaoLidas(bruno);
		long naoLidasAntesOutroPerfil = contarNaoLidas(outroPerfil);
		String mensagem = mensagemUnica();

		UUID id = notificacoes.notificarPerfil(Perfil.EQUIPE_TECNICA,
				new NovaNotificacao("TESTE_PERFIL", mensagem, "/lista-espera"));

		assertThat(contarNaoLidas(ana)).isEqualTo(naoLidasAntesAna + 1);
		assertThat(contarNaoLidas(bruno)).isEqualTo(naoLidasAntesBruno + 1);
		assertThat(contarNaoLidas(outroPerfil)).isEqualTo(naoLidasAntesOutroPerfil);
		mvc.perform(get("/api/notificacoes").param("size", "100").with(ana))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[?(@.id == '%s')].mensagem", id).value(hasItem(mensagem)))
			.andExpect(jsonPath("$.content[?(@.id == '%s')].tipo", id).value(hasItem("TESTE_PERFIL")))
			.andExpect(jsonPath("$.content[?(@.id == '%s')].referencia", id).value(hasItem("/lista-espera")))
			.andExpect(jsonPath("$.content[?(@.id == '%s')].lida", id).value(hasItem(false)));
		mvc.perform(get("/api/notificacoes").param("size", "100").with(outroPerfil))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id").value(not(hasItem(id.toString()))));

		mvc.perform(post("/api/notificacoes/{id}/leitura", id).with(ana).with(csrf()))
			.andExpect(status().isNoContent());

		assertThat(contarNaoLidas(ana)).isEqualTo(naoLidasAntesAna);
		assertThat(contarNaoLidas(bruno)).isEqualTo(naoLidasAntesBruno + 1);
		mvc.perform(get("/api/notificacoes").param("size", "100").with(ana))
			.andExpect(jsonPath("$.content[?(@.id == '%s')].lida", id).value(hasItem(true)));
		mvc.perform(get("/api/notificacoes").param("naoLidas", "true").param("size", "100").with(ana))
			.andExpect(jsonPath("$.content[*].id").value(not(hasItem(id.toString()))));
		mvc.perform(get("/api/notificacoes").param("naoLidas", "true").param("size", "100").with(bruno))
			.andExpect(jsonPath("$.content[*].id").value(hasItem(id.toString())));

		mvc.perform(post("/api/notificacoes/{id}/leitura", id).with(bruno).with(csrf()))
			.andExpect(status().isNoContent());
		assertThat(contarNaoLidas(bruno)).isEqualTo(naoLidasAntesBruno);
	}

	@Test
	void notificacaoDeContaSoChegaAEssaConta() throws Exception {
		UUID contaId = UUID.randomUUID();
		RequestPostProcessor destinataria = conta(contaId, Perfil.CONSULTA);
		RequestPostProcessor mesmoPerfil = novaConta(Perfil.CONSULTA);
		long naoLidasAntesMesmoPerfil = contarNaoLidas(mesmoPerfil);

		UUID id = notificacoes.notificarConta(contaId, NovaNotificacao.de("TESTE_CONTA", mensagemUnica()));

		assertThat(contarNaoLidas(destinataria)).isEqualTo(1);
		assertThat(contarNaoLidas(mesmoPerfil)).isEqualTo(naoLidasAntesMesmoPerfil);
		mvc.perform(get("/api/notificacoes").param("size", "100").with(mesmoPerfil))
			.andExpect(jsonPath("$.content[*].id").value(not(hasItem(id.toString()))));
	}

	@Test
	void contaNaoMarcaNotificacaoDeOutraContaNemDeOutroPerfil() throws Exception {
		UUID contaId = UUID.randomUUID();
		UUID daConta = notificacoes.notificarConta(contaId, NovaNotificacao.de("TESTE_CONTA", mensagemUnica()));
		UUID doPerfil = notificacoes.notificarPerfil(Perfil.COORDENADOR,
				NovaNotificacao.de("TESTE_PERFIL", mensagemUnica()));
		RequestPostProcessor intrusa = novaConta(Perfil.EQUIPE_TECNICA);
		clearInvocations(auditoria);

		mvc.perform(post("/api/notificacoes/{id}/leitura", daConta).with(intrusa).with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Recurso não encontrado"));
		mvc.perform(post("/api/notificacoes/{id}/leitura", doPerfil).with(intrusa).with(csrf()))
			.andExpect(status().isNotFound());
		mvc.perform(post("/api/notificacoes/{id}/leitura", UUID.randomUUID()).with(intrusa).with(csrf()))
			.andExpect(status().isNotFound());

		assertThat(contarNaoLidas(conta(contaId, Perfil.CONSULTA))).isEqualTo(1);
		verify(auditoria, never()).registrar(argThat(evento -> evento.acao() == AcaoAuditoria.ALTERAR));
	}

	@Test
	void marcarTodasComoLidasValeSoParaAPropriaConta() throws Exception {
		UUID contaId = UUID.randomUUID();
		RequestPostProcessor destinataria = conta(contaId, Perfil.ADMINISTRATIVO);
		RequestPostProcessor colega = novaConta(Perfil.ADMINISTRATIVO);
		notificacoes.notificarConta(contaId, NovaNotificacao.de("TESTE_CONTA", mensagemUnica()));
		UUID doPerfil = notificacoes.notificarPerfil(Perfil.ADMINISTRATIVO,
				NovaNotificacao.de("TESTE_PERFIL", mensagemUnica()));
		long naoLidasAntes = contarNaoLidas(destinataria);
		long naoLidasColega = contarNaoLidas(colega);
		assertThat(naoLidasAntes).isGreaterThanOrEqualTo(2);

		mvc.perform(post("/api/notificacoes/leitura").with(destinataria).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.marcadas").value((int) naoLidasAntes));

		assertThat(contarNaoLidas(destinataria)).isZero();
		assertThat(contarNaoLidas(colega)).isEqualTo(naoLidasColega);
		mvc.perform(get("/api/notificacoes").param("naoLidas", "true").param("size", "100").with(colega))
			.andExpect(jsonPath("$.content[*].id").value(hasItem(doPerfil.toString())));

		mvc.perform(post("/api/notificacoes/leitura").with(destinataria).with(csrf()))
			.andExpect(jsonPath("$.marcadas").value(0));
	}

	@Test
	void marcarComoLidaDuasVezesNaoFalhaENemAuditaDeNovo() throws Exception {
		UUID contaId = UUID.randomUUID();
		RequestPostProcessor destinataria = conta(contaId, Perfil.CONSULTA);
		UUID id = notificacoes.notificarConta(contaId, NovaNotificacao.de("TESTE_CONTA", mensagemUnica()));

		mvc.perform(post("/api/notificacoes/{id}/leitura", id).with(destinataria).with(csrf()))
			.andExpect(status().isNoContent());
		mvc.perform(post("/api/notificacoes/{id}/leitura", id).with(destinataria).with(csrf()))
			.andExpect(status().isNoContent());

		verify(auditoria, times(1)).registrar(argThat(evento -> evento.acao() == AcaoAuditoria.ALTERAR
				&& "notificacao".equals(evento.entidade()) && id.toString().equals(evento.entidadeId())
				&& evento.camposAlterados().equals(Set.of("lida_em"))));
	}

	@Test
	void criacaoEAuditadaUmaVezPorNotificacao() {
		List<UUID> ids = notificacoes.notificarPerfis(Set.of(Perfil.ADMINISTRATIVO, Perfil.COORDENADOR),
				new NovaNotificacao("VAGA_ABERTA", mensagemUnica(), "/lista-espera"));

		assertThat(ids).hasSize(2).doesNotHaveDuplicates();
		for (UUID id : ids) {
			verify(auditoria).registrar(argThat(evento -> evento.acao() == AcaoAuditoria.CRIAR
					&& "notificacao".equals(evento.entidade()) && id.toString().equals(evento.entidadeId())));
		}
	}

	@Test
	void listaPaginadaDaMaisNovaParaAMaisAntiga() throws Exception {
		UUID contaId = UUID.randomUUID();
		RequestPostProcessor destinataria = conta(contaId, Perfil.CONSULTA);
		UUID primeira = notificacoes.notificarConta(contaId, NovaNotificacao.de("TESTE_CONTA", mensagemUnica()));
		UUID segunda = notificacoes.notificarConta(contaId, NovaNotificacao.de("TESTE_CONTA", mensagemUnica()));
		UUID terceira = notificacoes.notificarConta(contaId, NovaNotificacao.de("TESTE_CONTA", mensagemUnica()));

		String corpo = mvc.perform(get("/api/notificacoes").param("size", "100").with(destinataria))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.size").value(100))
			.andReturn()
			.getResponse()
			.getContentAsString();
		List<String> ids = JsonPath.read(corpo, "$.content[*].id");
		assertThat(ids).containsSubsequence(terceira.toString(), segunda.toString(), primeira.toString());

		mvc.perform(get("/api/notificacoes").param("size", "1").with(destinataria))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.page.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
	}

	@Test
	void contaSemPermissaoRecebe403EmTodosOsEndpoints() throws Exception {
		RequestPostProcessor semPermissao = contaSemPermissoes();
		UUID qualquer = UUID.randomUUID();

		mvc.perform(get("/api/notificacoes").with(semPermissao)).andExpect(status().isForbidden());
		mvc.perform(get("/api/notificacoes/nao-lidas/contagem").with(semPermissao))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/notificacoes/{id}/leitura", qualquer).with(semPermissao).with(csrf()))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/notificacoes/leitura").with(semPermissao).with(csrf()))
			.andExpect(status().isForbidden());
	}

	@Test
	void todosOsPerfisTemNotificacoesLer() {
		// A matriz da seção 6 dá NOTIFICACOES_LER a todos os perfis; se isso mudar, o 403 acima cobre o perfil.
		assertThat(Perfil.values()).allMatch(perfil -> perfil.tem(Permissao.NOTIFICACOES_LER));
	}

	@Test
	void requisicaoSemContaNaoAcessa() throws Exception {
		mvc.perform(get("/api/notificacoes/nao-lidas/contagem"))
			.andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 401, 403));
	}

	private long contarNaoLidas(RequestPostProcessor conta) throws Exception {
		String corpo = mvc.perform(get("/api/notificacoes/nao-lidas/contagem").with(conta))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(corpo, "$.quantidade")).longValue();
	}

	private static String mensagemUnica() {
		return "Mensagem de teste " + UUID.randomUUID();
	}

	private static RequestPostProcessor novaConta(Perfil perfil) {
		return conta(UUID.randomUUID(), perfil);
	}

	private static RequestPostProcessor conta(UUID id, Perfil perfil) {
		ContaDeTeste conta = new ContaDeTeste(id, "teste-" + id, "Conta de teste", perfil);
		List<SimpleGrantedAuthority> authorities = perfil.permissoes()
			.stream()
			.map(permissao -> new SimpleGrantedAuthority(permissao.name()))
			.toList();
		return authentication(UsernamePasswordAuthenticationToken.authenticated(conta, null, authorities));
	}

	private static RequestPostProcessor contaSemPermissoes() {
		UUID id = UUID.randomUUID();
		ContaDeTeste conta = new ContaDeTeste(id, "teste-" + id, "Conta de teste", Perfil.CONSULTA);
		return authentication(UsernamePasswordAuthenticationToken.authenticated(conta, null, List.of()));
	}

	record ContaDeTeste(UUID id, String login, String nome, Perfil perfil) implements ContaAutenticada {
	}

}
