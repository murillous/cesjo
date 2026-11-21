package br.org.cesjo.sgi.atendidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Registro de violação de direitos (issue #43): categorias do RF009, âmbito quando a categoria pede,
 * descrição cifrada, auditoria e acesso só para Coordenador e Equipe Técnica. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RegistroViolacaoTests {

	private static final String HOJE = AtendidosDeTeste.HOJE.toString();

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@AfterEach
	void restaurarAuditoria() {
		reset(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "violacao-tecnica")
	void equipeTecnicaRegistraComDescricaoCifrada() throws Exception {
		String id = inserir();

		String corpo = registrar(id, dados("VIOLENCIA_FISICA_PSICOLOGICA", "INTRAFAMILIAR", HOJE, " Relato fictício. "))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.insercaoId").value(id))
			.andExpect(jsonPath("$.tipo").value("VIOLENCIA_FISICA_PSICOLOGICA"))
			.andExpect(jsonPath("$.ambito").value("INTRAFAMILIAR"))
			.andExpect(jsonPath("$.descricao").doesNotExist())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String violacaoId = JsonPath.read(corpo, "$.id");

		String gravada = jdbc.queryForObject("select descricao from violacao_direito where id = ?::uuid", String.class,
				violacaoId);
		assertThat(gravada).startsWith("v1:").doesNotContain("Relato");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR
				&& e.entidade().equals("violacao_direito") && e.entidadeId().equals(violacaoId)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "violacao-coord")
	void ambitoSoNasCategoriasQueOPedem() throws Exception {
		String id = inserir();

		registrar(id, dados("ISOLAMENTO_SOCIAL", null, HOJE, "Relato fictício.")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.ambito").doesNotExist());
		registrar(id, dados("ABUSO_SEXUAL", null, HOJE, "Relato fictício.")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("ambitoDaCategoria")));
		registrar(id, dados("CONFINAMENTO", "EXTRAFAMILIAR", HOJE, "Relato fictício.")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("ambitoDaCategoria")));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "violacao-invalida")
	void recusaDadosInvalidos() throws Exception {
		String id = inserir();
		registrar(id, dados(null, null, AtendidosDeTeste.HOJE.plusDays(1).toString(), " ")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("tipo")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("dataIdentificacao")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("descricao")));
		registrar(UUID.randomUUID().toString(), dados("OUTRAS", null, HOJE, "Relato fictício."))
			.andExpect(status().isNotFound());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "violacao-admin")
	void administrativoNaoRegistra() throws Exception {
		registrar(UUID.randomUUID().toString(), dados("OUTRAS", null, HOJE, "Relato fictício."))
			.andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "violacao-consulta")
	void consultaNaoRegistra() throws Exception {
		registrar(UUID.randomUUID().toString(), dados("OUTRAS", null, HOJE, "Relato fictício."))
			.andExpect(status().isForbidden());
	}

	private String inserir() throws Exception {
		return new AtendidosDeTeste(mvc, jdbc).inserirNoServico("Pessoa Fictícia Violação", null, "INTELECTUAL",
				"MATUTINO");
	}

	private ResultActions registrar(String id, Map<String, Object> dados) throws Exception {
		return mvc.perform(post("/api/atendidos/{id}/violacoes", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(dados)));
	}

	private static Map<String, Object> dados(String tipo, String ambito, String data, String descricao) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("tipo", tipo);
		dados.put("ambito", ambito);
		dados.put("dataIdentificacao", data);
		dados.put("descricao", descricao);
		return dados;
	}

}
