package br.org.cesjo.sgi.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.springframework.test.web.servlet.ResultActions;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reuniões e eventos institucionais (issue #52, ADR 0175 e ADR 0176): os sete tipos, a indicação de
 * realizado, recebido ou participado, a ligação opcional a parceiro, filtros, cifra da descrição,
 * permissões e auditoria.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EventosInstitucionaisTests {

	private static final String URL = "/api/eventos-institucionais";

	@Autowired
	MockMvc mvc;

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
	@ComConta(perfil = Perfil.COORDENADOR, login = "evento-coord-registra")
	void registraVisitaRecebidaLigadaAParceiroComDescricaoCifrada() throws Exception {
		String parceiro = criarParceiro("CREAS Fictício");
		Map<String, Object> dados = evento("VISITA_INSTITUCIONAL", "RECEBIDO", "2026-04-15",
				"Visita técnica do CREAS para conhecer o serviço.");
		dados.put("participantes", "Coordenação, equipe técnica e duas técnicas do CREAS");
		dados.put("parceiroId", parceiro);
		String id = registrar(dados);

		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tipo").value("VISITA_INSTITUCIONAL"))
			.andExpect(jsonPath("$.direcao").value("RECEBIDO"))
			.andExpect(jsonPath("$.data").value("2026-04-15"))
			.andExpect(jsonPath("$.descricao").value("Visita técnica do CREAS para conhecer o serviço."))
			.andExpect(jsonPath("$.participantes").value("Coordenação, equipe técnica e duas técnicas do CREAS"))
			.andExpect(jsonPath("$.parceiro.id").value(parceiro))
			.andExpect(jsonPath("$.parceiro.nome").value("CREAS Fictício"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR
				&& e.entidade().equals("evento_institucional") && e.entidadeId().equals(id)));

		String gravada = jdbc.queryForObject("select descricao from evento_institucional where id = ?", String.class,
				UUID.fromString(id));
		assertThat(gravada).doesNotContain("CREAS");
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "evento-coord-tipos")
	void aceitaOsSeteTiposComADirecaoCerta() throws Exception {
		for (TipoEventoInstitucional tipo : TipoEventoInstitucional.values()) {
			String direcao = tipo.direcoes().isEmpty() ? null : tipo.direcoes().iterator().next().name();
			registrar(evento(tipo.name(), direcao, "2026-02-01", "Registro do tipo " + tipo));
		}

		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(evento("EVENTO", "RECEBIDO", "2026-02-01", "Evento recebido não existe."))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("direcaoDoTipo")));
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(evento("VISITA_INSTITUCIONAL", null, "2026-02-01", "Visita sem indicação."))))
			.andExpect(status().isBadRequest());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(evento("REUNIAO_DIRECAO_GERAL", "REALIZADO", "2026-02-01", "Reunião com direção."))))
			.andExpect(status().isBadRequest());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "evento-coord-edita")
	void edicaoAuditaSoOsCamposAlterados() throws Exception {
		String id = registrar(evento("REUNIAO_COORDENACAO_EQUIPE", null, "2026-03-02", "Pauta do mês."));
		clearInvocations(auditoria);

		Map<String, Object> edicao = evento("REUNIAO_COORDENACAO_EQUIPE", null, "2026-03-03", "Pauta do mês.");
		edicao.put("participantes", "Equipe técnica");
		editar(id, edicao).andExpect(status().isOk()).andExpect(jsonPath("$.data").value("2026-03-03"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR && e.entidadeId().equals(id)
				&& e.camposAlterados().equals(Set.of("data", "participantes"))));

		clearInvocations(auditoria);
		editar(id, edicao).andExpect(status().isOk());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "evento-coord-filtra")
	void filtraPorTipoPeriodoEParceiro() throws Exception {
		String parceiro = criarParceiro("Parceiro Filtro Fictício");
		Map<String, Object> comParceiro = evento("REUNIAO_PARCEIROS", null, "2025-07-10", "Reunião de parceria.");
		comParceiro.put("parceiroId", parceiro);
		String reuniao = registrar(comParceiro);
		String formacao = registrar(evento("FORMACAO_MULTIPROFISSIONAL", null, "2025-07-20", "Formação."));
		String fora = registrar(evento("FORMACAO_MULTIPROFISSIONAL", null, "2025-09-01", "Outra formação."));

		mvc.perform(get(URL).param("de", "2025-07-01").param("ate", "2025-07-31").param("size", "1000"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id").value(hasItem(reuniao)))
			.andExpect(jsonPath("$.content[*].id").value(hasItem(formacao)))
			.andExpect(jsonPath("$.content[*].id").value(not(hasItem(fora))));
		mvc.perform(get(URL).param("tipo", "FORMACAO_MULTIPROFISSIONAL").param("size", "1000"))
			.andExpect(jsonPath("$.content[*].tipo").value(everyItem(is("FORMACAO_MULTIPROFISSIONAL"))))
			.andExpect(jsonPath("$.content[*].id").value(hasItem(fora)));
		mvc.perform(get(URL).param("parceiroId", parceiro))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(reuniao));
		mvc.perform(get(URL).param("de", "2025-08-01").param("ate", "2025-07-01"))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "evento-coord-invalido")
	void recusaDadosInvalidos() throws Exception {
		Map<String, Object> futuro = evento("EVENTO", "PARTICIPADO", LocalDate.now().plusDays(3).toString(), "Futuro.");
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(futuro)))
			.andExpect(status().isBadRequest());
		Map<String, Object> vazio = evento(null, null, null, " ");
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(vazio)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("tipo")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("data")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("descricao")));
		Map<String, Object> semParceiro = evento("REUNIAO_PARCEIROS", null, "2026-01-01", "Parceiro inexistente.");
		semParceiro.put("parceiroId", UUID.randomUUID().toString());
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(semParceiro)))
			.andExpect(status().isUnprocessableContent());
		editar(UUID.randomUUID().toString(), evento("EVENTO", "REALIZADO", "2026-01-01", "Inexistente."))
			.andExpect(status().isNotFound());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "evento-tecnica")
	void equipeTecnicaSemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "evento-admin")
	void administrativoSemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "evento-consulta")
	void consultaSemAcesso() throws Exception {
		semAcesso();
	}

	private void semAcesso() throws Exception {
		String id = UUID.randomUUID().toString();
		Map<String, Object> dados = evento("EVENTO", "REALIZADO", "2026-01-01", "Evento.");
		mvc.perform(get(URL)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}", id)).andExpect(status().isForbidden());
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isForbidden());
		editar(id, dados).andExpect(status().isForbidden());
	}

	private static Map<String, Object> evento(String tipo, String direcao, String data, String descricao) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("tipo", tipo);
		dados.put("direcao", direcao);
		dados.put("data", data);
		dados.put("descricao", descricao);
		return dados;
	}

	private String registrar(Map<String, Object> dados) throws Exception {
		String corpo = mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private String criarParceiro(String nome) throws Exception {
		String corpo = mvc.perform(post("/api/parceiros").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("nome", nome, "tipo", "ORGAO_PUBLICO"))))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		clearInvocations(auditoria);
		return JsonPath.read(corpo, "$.id");
	}

	private ResultActions editar(String id, Map<String, Object> dados) throws Exception {
		return mvc.perform(put(URL + "/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)));
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
