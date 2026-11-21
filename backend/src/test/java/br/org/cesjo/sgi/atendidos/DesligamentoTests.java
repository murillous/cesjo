package br.org.cesjo.sgi.atendidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import tools.jackson.databind.json.JsonMapper;

/**
 * Desligamento do usuário atendido (issue #42): a inserção passa a desligada, a vaga é liberada por
 * evento, a observação é cifrada e a leitura dela é auditada. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class DesligamentoTests {

	private static final LocalDate HOJE = AtendidosDeTeste.HOJE;

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	UsuariosAtendidos usuariosAtendidos;

	@Autowired
	ApplicationEvents eventos;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@AfterEach
	void restaurarAuditoria() {
		reset(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "desligamento-coord")
	void desligaLiberaAVagaECifraAObservacao() throws Exception {
		String id = inserir();
		long ativosAntes = usuariosAtendidos.contarAtivos();
		clearInvocations(auditoria);

		desligar(id, dados(HOJE.toString(), "MUDANCA_MUNICIPIO", "  Família mudou para outra cidade fictícia. "))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.situacao").value("DESLIGADA"))
			.andExpect(jsonPath("$.motivo").value("MUDANCA_MUNICIPIO"))
			.andExpect(jsonPath("$.observacao").doesNotExist());

		assertThat(usuariosAtendidos.contarAtivos()).as("a vaga é liberada").isEqualTo(ativosAntes - 1);
		assertThat(eventos.stream(UsuarioDesligado.class)).singleElement()
			.satisfies(evento -> assertThat(evento.insercaoId()).hasToString(id))
			.satisfies(evento -> assertThat(evento.data()).isEqualTo(HOJE));
		String gravada = jdbc.queryForObject("select observacao from desligamento where insercao_id = ?::uuid",
				String.class, id);
		assertThat(gravada).startsWith("v1:").doesNotContain("Família");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("desligamento")));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR && e.entidade().equals("insercao")
				&& e.entidadeId().equals(id) && e.camposAlterados().contains("situacao")));
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL));

		mvc.perform(get("/api/atendidos/{id}", id))
			.andExpect(jsonPath("$.situacao").value("DESLIGADA"))
			.andExpect(jsonPath("$.dataDesligamento").doesNotExist())
			.andExpect(jsonPath("$.desligamento.data").value(HOJE.toString()))
			.andExpect(jsonPath("$.desligamento.motivo").value("MUDANCA_MUNICIPIO"))
			.andExpect(jsonPath("$.desligamento.observacao").value("Família mudou para outra cidade fictícia."));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL
				&& e.entidade().equals("desligamento")));
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "desligamento-consulta")
	void consultaVeADataDoDesligamentoMasNaoDesliga() throws Exception {
		AtendidosDeTeste atendidos = new AtendidosDeTeste(mvc, jdbc);
		String nome = "Teste" + UUID.randomUUID().toString().substring(0, 8);
		String id = atendidos.inserirNoServico(nome, null, "TEA", "MATUTINO");

		desligar(id, dados(HOJE.toString(), "EVASAO", null)).andExpect(status().isForbidden());
		atendidos.criar("/api/atendidos/" + id + "/desligamento", dados(HOJE.toString(), "EVASAO", null));

		mvc.perform(get("/api/atendidos").param("nome", nome))
			.andExpect(jsonPath("$.content[0].situacao").value("DESLIGADA"))
			.andExpect(jsonPath("$.content[0].dataDesligamento").value(HOJE.toString()));
		mvc.perform(get("/api/atendidos/{id}", id))
			.andExpect(jsonPath("$.dataDesligamento").value(HOJE.toString()))
			.andExpect(jsonPath("$.desligamento").doesNotExist());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "desligamento-regras")
	void recusaRegrasDeNegocioEDadosInvalidos() throws Exception {
		String id = inserir();

		desligar(id, dados(HOJE.minusDays(5).toString(), "EVASAO", null)).andExpect(status().isUnprocessableContent());
		desligar(id, dados(HOJE.plusDays(1).toString(), "OUTROS", " ")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("data")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("observacaoDoMotivo")));
		desligar(UUID.randomUUID().toString(), dados(HOJE.toString(), "EVASAO", null)).andExpect(status().isNotFound());
		assertThat(eventos.stream(UsuarioDesligado.class)).isEmpty();

		desligar(id, dados(HOJE.toString(), "OUTROS", "Motivo fictício.")).andExpect(status().isCreated());
		desligar(id, dados(HOJE.toString(), "EVASAO", null)).andExpect(status().isUnprocessableContent());
		assertThat(eventos.stream(UsuarioDesligado.class)).hasSize(1);
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "desligamento-tecnica")
	void equipeTecnicaNaoDesliga() throws Exception {
		desligar(UUID.randomUUID().toString(), dados(HOJE.toString(), "EVASAO", null)).andExpect(status().isForbidden());
	}

	private String inserir() throws Exception {
		return new AtendidosDeTeste(mvc, jdbc).inserirNoServico("Pessoa Fictícia Desligada", null, "FISICA",
				"VESPERTINO");
	}

	private ResultActions desligar(String id, Map<String, Object> dados) throws Exception {
		return mvc.perform(post("/api/atendidos/{id}/desligamento", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(dados)));
	}

	private static Map<String, Object> dados(String data, String motivo, String observacao) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("data", data);
		dados.put("motivo", motivo);
		dados.put("observacao", observacao);
		return dados;
	}

}
