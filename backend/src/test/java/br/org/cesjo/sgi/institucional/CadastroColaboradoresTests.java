package br.org.cesjo.sgi.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cadastro de colaboradores (issue #50): API HTTP, permissões, ligação com a conta de acesso,
 * auditoria e a API Java usada por visitas, escutas e atividades. O Postgres dos testes é
 * compartilhado, então cada teste cria os próprios dados e não conta com tabela vazia (ADR 0008).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CadastroColaboradoresTests {

	private static final String URL = "/api/colaboradores";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	Colaboradores colaboradores;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "colab-coord-cria")
	void coordenadorCadastraColaboradorAtivoEAuditado() throws Exception {
		String id = criar(Map.of("nome", "  Colaboradora Fictícia ", "funcao", "ASSISTENTE_SOCIAL"));

		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value("Colaboradora Fictícia"))
			.andExpect(jsonPath("$.funcao").value("ASSISTENTE_SOCIAL"))
			.andExpect(jsonPath("$.ativo").value(true))
			.andExpect(jsonPath("$.conta").doesNotExist());
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR
				&& e.entidade().equals(Colaboradores.ENTIDADE) && e.entidadeId().equals(id)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "colab-coord-edita")
	void edicaoAuditaSoOsCamposAlterados() throws Exception {
		String id = criar(Map.of("nome", "Colaborador Fictício", "funcao", "PSICOLOGO"));
		clearInvocations(auditoria);

		mvc.perform(put(URL + "/{id}", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(edicao("Colaborador Fictício", "TERAPEUTA_OCUPACIONAL", false, null))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.funcao").value("TERAPEUTA_OCUPACIONAL"))
			.andExpect(jsonPath("$.ativo").value(false));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR && e.entidadeId().equals(id)
				&& e.camposAlterados().equals(Set.of("funcao", "ativo"))));

		clearInvocations(auditoria);
		mvc.perform(put(URL + "/{id}", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(edicao("Colaborador Fictício", "TERAPEUTA_OCUPACIONAL", false, null))))
			.andExpect(status().isOk());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "colab-coord-lista")
	void listaFiltraPorSituacao() throws Exception {
		String ativo = criar(Map.of("nome", "Ativo Fictício", "funcao", "OUTRA"));
		String inativo = criar(Map.of("nome", "Inativo Fictício", "funcao", "OUTRA"));
		mvc.perform(put(URL + "/{id}", inativo).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(edicao("Inativo Fictício", "OUTRA", false, null))))
			.andExpect(status().isOk());

		mvc.perform(get(URL).param("ativo", "true").param("size", "1000"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id").value(hasItem(ativo)))
			.andExpect(jsonPath("$.content[*].id").value(not(hasItem(inativo))));
		mvc.perform(get(URL).param("ativo", "false").param("size", "1000"))
			.andExpect(jsonPath("$.content[*].id").value(hasItem(inativo)))
			.andExpect(jsonPath("$.content[*].id").value(not(hasItem(ativo))));
		mvc.perform(get(URL).param("size", "1000"))
			.andExpect(jsonPath("$.content[*].id").value(hasItem(ativo)))
			.andExpect(jsonPath("$.content[*].id").value(hasItem(inativo)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "colab-coord-conta")
	void ligaEDesligaContaDeAcesso() throws Exception {
		UUID conta = novaConta();
		String id = criar(Map.of("nome", "Colaborador Com Conta", "funcao", "PSICOLOGO", "contaId", conta.toString()));

		mvc.perform(get(URL + "/{id}", id))
			.andExpect(jsonPath("$.conta.id").value(conta.toString()))
			.andExpect(jsonPath("$.conta.login").value(startsWith("colab")))
			.andExpect(jsonPath("$.conta.ativa").value(true));

		clearInvocations(auditoria);
		mvc.perform(put(URL + "/{id}", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(edicao("Colaborador Com Conta", "PSICOLOGO", true, null))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.conta").doesNotExist());
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.camposAlterados().equals(Set.of("conta_acesso_id"))));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "colab-coord-conta-dupla")
	void contaLigaANoMaximoUmColaborador() throws Exception {
		UUID conta = novaConta();
		criar(Map.of("nome", "Primeiro Fictício", "funcao", "OUTRA", "contaId", conta.toString()));
		clearInvocations(auditoria);

		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("nome", "Segundo Fictício", "funcao", "OUTRA", "contaId", conta.toString()))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Esta conta de acesso já está ligada a outro colaborador."));
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("nome", "Terceiro Fictício", "funcao", "OUTRA", "contaId",
					UUID.randomUUID().toString()))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("A conta de acesso escolhida não existe."));
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "colab-coord-invalido")
	void recusaDadosInvalidos() throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("nome", " ");
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("nome")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("funcao")));
		mvc.perform(put(URL + "/{id}", UUID.randomUUID()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(edicao("Fictício", "OUTRA", true, null))))
			.andExpect(status().isNotFound());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "colab-tecnica")
	void equipeTecnicaNaoVeNemGerencia() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "colab-admin")
	void administrativoNaoVeNemGerencia() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "colab-consulta")
	void consultaNaoVeNemGerencia() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "colab-coord-api-java")
	void apiJavaSoOfereceColaboradoresAtivos() throws Exception {
		UUID conta = novaConta();
		UUID ativo = UUID.fromString(
				criar(Map.of("nome", "Profissional Ativo", "funcao", "ASSISTENTE_SOCIAL", "contaId", conta.toString())));
		UUID inativo = UUID.fromString(criar(Map.of("nome", "Profissional Inativo", "funcao", "PSICOLOGO")));
		mvc.perform(put(URL + "/{id}", inativo).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(edicao("Profissional Inativo", "PSICOLOGO", false, null))))
			.andExpect(status().isOk());

		assertThat(colaboradores.listarAtivos()).extracting(ResumoColaborador::id).contains(ativo).doesNotContain(inativo);
		assertThat(colaboradores.obterAtivo(ativo))
			.contains(new ResumoColaborador(ativo, "Profissional Ativo", FuncaoColaborador.ASSISTENTE_SOCIAL));
		assertThat(colaboradores.obterAtivo(inativo)).isEmpty();
		assertThat(colaboradores.obterAtivoPorConta(conta)).map(ResumoColaborador::id).contains(ativo);

		colaboradores.exigirAtivos(List.of(ativo, ativo));
		assertThatThrownBy(() -> colaboradores.exigirAtivos(List.of(ativo, inativo)))
			.isInstanceOf(ColaboradorIndisponivelException.class);
		assertThatThrownBy(() -> colaboradores.exigirAtivos(List.of(UUID.randomUUID())))
			.isInstanceOf(ColaboradorIndisponivelException.class);
	}

	private void semAcesso() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("nome", "Fictício", "funcao", "OUTRA"))))
			.andExpect(status().isForbidden());
		mvc.perform(put(URL + "/{id}", UUID.randomUUID()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(edicao("Fictício", "OUTRA", true, null))))
			.andExpect(status().isForbidden());
	}

	private String criar(Map<String, Object> dados) throws Exception {
		String corpo = mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith(URL + "/")))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private static Map<String, Object> edicao(String nome, String funcao, boolean ativo, UUID contaId) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("nome", nome);
		dados.put("funcao", funcao);
		dados.put("ativo", ativo);
		dados.put("contaId", contaId);
		return dados;
	}

	/** Conta de acesso fictícia gravada direto no banco, com login único. */
	private UUID novaConta() {
		UUID id = UUID.randomUUID();
		Instant agora = Instant.now();
		jdbc.update("""
				insert into conta_acesso (id, login, nome, senha_hash, perfil, criado_em, atualizado_em)
				values (?, ?, 'Conta Fictícia', 'hash-ficticio', 'EQUIPE_TECNICA', ?, ?)""", id,
				"colab" + id.toString().replace("-", "").substring(0, 12), java.sql.Timestamp.from(agora),
				java.sql.Timestamp.from(agora));
		return id;
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
