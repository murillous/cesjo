package br.org.cesjo.sgi.atendimento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
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

/** Escuta ativa e qualificada (issue #36): textos cifrados, leitura auditada e permissões. */
@Import({ TestcontainersConfiguration.class, DadosDeVisita.class })
@SpringBootTest
@AutoConfigureMockMvc
class EscutasTests {

	private static final String URL = "/api/escutas";

	private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

	@Autowired
	MockMvc mvc;

	@Autowired
	DadosDeVisita dados;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "escuta-tecnica-registra")
	void registraEscutaCifradaELeituraAuditada() throws Exception {
		UUID pessoa = dados.candidato("Pessoa Escutada", "EM_AVALIACAO");
		UUID profissional = dados.colaborador("Psicóloga Fictícia", true);

		String corpo = mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", pessoa, "dados",
					escuta(HOJE.minusDays(1), profissional, " Mãe e avó dividem os cuidados (fictício). ")))))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith(URL + "/")))
			.andExpect(jsonPath("$.nomePessoa").value("Pessoa Escutada"))
			.andExpect(jsonPath("$.profissional.nome").value("Psicóloga Fictícia"))
			.andExpect(jsonPath("$.dinamicaFamiliar").value("Mãe e avó dividem os cuidados (fictício)."))
			.andExpect(jsonPath("$.habilidades").doesNotExist())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String id = JsonPath.read(corpo, "$.id");

		String noBanco = jdbc.queryForObject("select dinamica_familiar from escuta where id = ?::uuid", String.class, id);
		assertThat(noBanco).startsWith("v1:").doesNotContain("avó");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("escuta")));

		mvc.perform(get(URL).param("pessoaId", pessoa.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(id))
			.andExpect(jsonPath("$[0].dinamicaFamiliar").doesNotExist());
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL));

		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.dinamicaFamiliar").value("Mãe e avó dividem os cuidados (fictício)."));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL
				&& e.entidade().equals("escuta") && e.entidadeId().equals(id)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "escuta-coord-corrige")
	void corrigeEAuditaSoOsCamposAlterados() throws Exception {
		UUID pessoa = dados.candidato("Pessoa Corrigida", "EM_AVALIACAO");
		UUID profissional = dados.colaborador("Assistente Corrige", true);
		String id = registrar(pessoa, escuta(HOJE, profissional, "Texto original"));
		clearInvocations(auditoria);

		Map<String, Object> corrigida = escuta(HOJE, profissional, "Texto original");
		corrigida.put("habilidades", "Gosta de música (fictício)");
		mvc.perform(put(URL + "/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(corrigida)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.habilidades").value("Gosta de música (fictício)"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.camposAlterados().equals(Set.of("habilidades"))));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "escuta-tecnica-regras")
	void recusaEscutaVaziaFuturaOuComProfissionalInativo() throws Exception {
		UUID pessoa = dados.candidato("Pessoa Regras Escuta", "AGUARDANDO");
		UUID ativo = dados.colaborador("Ativo Escuta", true);
		UUID inativo = dados.colaborador("Inativo Escuta", false);

		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", pessoa, "dados", escuta(HOJE.plusDays(1), ativo, " ")))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("dados.comConteudo")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("dados.dataEscuta")));
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", pessoa, "dados", escuta(HOJE, inativo, "Texto")))))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", UUID.randomUUID(), "dados", escuta(HOJE, ativo, "Texto")))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("A pessoa escolhida não existe."));
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "escuta-coord-pessoas")
	void ofereceCandidatosEProfissionais() throws Exception {
		UUID pessoa = dados.candidato("Pessoa Para Escuta", "AGUARDANDO");
		mvc.perform(get("/api/atendimento/pessoas"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id").value(hasItem(pessoa.toString())));
		mvc.perform(get("/api/atendimento/pessoas/{id}", pessoa))
			.andExpect(jsonPath("$.nomeCompleto").value("Pessoa Para Escuta"));
		mvc.perform(get("/api/atendimento/pessoas/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
		mvc.perform(get(URL + "/profissionais")).andExpect(status().isOk()).andExpect(jsonPath("$.profissionais").isArray());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "escuta-admin")
	void administrativoNaoTemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "escuta-consulta")
	void consultaNaoTemAcesso() throws Exception {
		semAcesso();
	}

	private void semAcesso() throws Exception {
		mvc.perform(get(URL).param("pessoaId", UUID.randomUUID().toString())).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/profissionais")).andExpect(status().isForbidden());
		mvc.perform(get("/api/atendimento/pessoas")).andExpect(status().isForbidden());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", UUID.randomUUID(), "dados", escuta(HOJE, UUID.randomUUID(), "x")))))
			.andExpect(status().isForbidden());
	}

	private String registrar(UUID pessoa, Map<String, Object> dadosEscuta) throws Exception {
		String corpo = mvc
			.perform(post(URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("pessoaId", pessoa, "dados", dadosEscuta))))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private static Map<String, Object> escuta(LocalDate data, UUID profissional, String dinamica) {
		Map<String, Object> mapa = new LinkedHashMap<>();
		mapa.put("dataEscuta", data.toString());
		mapa.put("profissionalId", profissional);
		mapa.put("dinamicaFamiliar", dinamica);
		return mapa;
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
