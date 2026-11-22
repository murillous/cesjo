package br.org.cesjo.sgi.atendidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;

/**
 * Consulta de violações de direitos (issue #44, ADR 0145): lista por usuário e geral, com filtro por
 * categoria e período, sem a descrição; ler a descrição é auditado. O banco é compartilhado, então
 * cada teste usa um período só dele. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ConsultaViolacoesTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	Auditoria auditoria;

	@AfterEach
	void restaurarAuditoria() {
		reset(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "consulta-violacao-tecnica")
	void listaDoAtendidoFiltraPorCategoriaEPeriodoSemADescricao() throws Exception {
		AtendidosDeTeste atendidos = new AtendidosDeTeste(mvc, jdbc);
		String id = atendidos.inserirNoServico("Pessoa Fictícia Consulta", null, "TEA", "MATUTINO");
		String outro = atendidos.inserirNoServico("Outra Pessoa Fictícia", null, "TEA", "MATUTINO");
		registrar(atendidos, id, "ISOLAMENTO_SOCIAL", null, "2020-01-10");
		registrar(atendidos, id, "ABUSO_SEXUAL", "EXTRAFAMILIAR", "2020-03-05");
		registrar(atendidos, id, "ISOLAMENTO_SOCIAL", null, "2020-06-20");
		registrar(atendidos, outro, "ISOLAMENTO_SOCIAL", null, "2020-03-01");
		clearInvocations(auditoria);

		mvc.perform(get("/api/atendidos/{id}/violacoes", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[0].dataIdentificacao").value("2020-06-20"))
			.andExpect(jsonPath("$[1].tipo").value("ABUSO_SEXUAL"))
			.andExpect(jsonPath("$[1].ambito").value("EXTRAFAMILIAR"))
			.andExpect(jsonPath("$[1].insercaoId").value(id))
			.andExpect(jsonPath("$[1].nomeCompleto").value("Pessoa Fictícia Consulta"))
			.andExpect(jsonPath("$[0].descricao").doesNotExist());
		mvc.perform(get("/api/atendidos/{id}/violacoes", id).param("tipo", "ISOLAMENTO_SOCIAL"))
			.andExpect(jsonPath("$.length()").value(2));
		mvc.perform(get("/api/atendidos/{id}/violacoes", id).param("de", "2020-03-05").param("ate", "2020-06-19"))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].tipo").value("ABUSO_SEXUAL"));
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "consulta-violacao-coord")
	void listaGeralPaginadaComFiltros() throws Exception {
		AtendidosDeTeste atendidos = new AtendidosDeTeste(mvc, jdbc);
		String id = atendidos.inserirNoServico("Pessoa Fictícia Geral", null, "TEA", "VESPERTINO");
		String outro = atendidos.inserirNoServico("Outra Pessoa Fictícia Geral", null, "TEA", "VESPERTINO");
		registrar(atendidos, id, "CONFINAMENTO", null, "2019-05-01");
		registrar(atendidos, outro, "CONFINAMENTO", null, "2019-05-02");
		registrar(atendidos, outro, "VIOLENCIA_PATRIMONIAL", null, "2019-05-03");

		mvc.perform(get("/api/violacoes").param("de", "2019-05-01").param("ate", "2019-05-31").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.totalElements").value(3))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].tipo").value("VIOLENCIA_PATRIMONIAL"))
			.andExpect(jsonPath("$.content[0].insercaoId").value(outro))
			.andExpect(jsonPath("$.content[0].descricao").doesNotExist());
		mvc.perform(get("/api/violacoes").param("tipo", "CONFINAMENTO").param("de", "2019-05-01").param("ate", "2019-05-31"))
			.andExpect(jsonPath("$.page.totalElements").value(2))
			.andExpect(jsonPath("$.content[1].nomeCompleto").value("Pessoa Fictícia Geral"));
		mvc.perform(get("/api/violacoes").param("de", "2019-06-01").param("ate", "2019-05-01"))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "consulta-violacao-leitura")
	void lerADescricaoEAuditado() throws Exception {
		AtendidosDeTeste atendidos = new AtendidosDeTeste(mvc, jdbc);
		String id = atendidos.inserirNoServico("Pessoa Fictícia Leitura", null, "TEA", "INTEGRAL");
		String violacao = registrar(atendidos, id, "NEGLIGENCIA_ABANDONO", null, "2021-02-02");
		clearInvocations(auditoria);

		mvc.perform(get("/api/violacoes/{id}", violacao))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.descricao").value("Relato fictício da consulta."))
			.andExpect(jsonPath("$.tipo").value("NEGLIGENCIA_ABANDONO"))
			.andExpect(jsonPath("$.ambito").value(nullValue()))
			.andExpect(jsonPath("$.registradaEm").isNotEmpty());
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL
				&& e.entidade().equals("violacao_direito") && e.entidadeId().equals(violacao)));
		mvc.perform(get("/api/violacoes/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
		assertThat(jdbc.queryForObject("select descricao from violacao_direito where id = ?::uuid", String.class,
				violacao))
			.startsWith("v1:");
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "consulta-violacao-admin")
	void administrativoNaoConsulta() throws Exception {
		exigirProibido();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "consulta-violacao-consulta")
	void consultaNaoConsulta() throws Exception {
		exigirProibido();
	}

	private void exigirProibido() throws Exception {
		UUID id = UUID.randomUUID();
		mvc.perform(get("/api/atendidos/{id}/violacoes", id)).andExpect(status().isForbidden());
		mvc.perform(get("/api/violacoes")).andExpect(status().isForbidden());
		mvc.perform(get("/api/violacoes/{id}", id)).andExpect(status().isForbidden());
	}

	/** Registra como Coordenador; devolve o id da violação. */
	private static String registrar(AtendidosDeTeste atendidos, String insercaoId, String tipo, String ambito,
			String data) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("tipo", tipo);
		dados.put("ambito", ambito);
		dados.put("dataIdentificacao", data);
		dados.put("descricao", "Relato fictício da consulta.");
		return JsonPath.read(atendidos.criar("/api/atendidos/" + insercaoId + "/violacoes", dados), "$.id");
	}

}
