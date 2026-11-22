package br.org.cesjo.sgi.atendidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;

/**
 * Fluxo inteiro da vaga (issue #94, ADR 0135): inserir no serviço ocupa a vaga, desligar a libera e o
 * aviso de vaga aberta sai para Coordenador e Administrativo. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class VagaLiberadaNoDesligamentoTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "vaga-desligamento")
	void inserirOcupaDesligarLiberaENotifica() throws Exception {
		AtendidosDeTeste atendidos = new AtendidosDeTeste(mvc, jdbc);
		long ocupadasAntes = numero("$.ocupadas");
		String insercao = atendidos.inserirNoServico("Pessoa Fictícia da Vaga", null, "TEA", "VESPERTINO");
		assertThat(numero("$.ocupadas")).as("a inserção ocupa a vaga").isEqualTo(ocupadasAntes + 1);

		definirCapacidade(ocupadasAntes + 1);
		mvc.perform(get("/api/lista-espera/vagas")).andExpect(jsonPath("$.disponiveis").value(0));
		long avisosAntes = avisos();

		mvc.perform(post("/api/atendidos/{id}/desligamento", insercao).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"data\": \"" + AtendidosDeTeste.HOJE + "\", \"motivo\": \"ALCANCE_OBJETIVOS\"}"))
			.andExpect(status().isCreated());

		mvc.perform(get("/api/lista-espera/vagas"))
			.andExpect(jsonPath("$.ocupadas").value(ocupadasAntes))
			.andExpect(jsonPath("$.disponiveis").value(1));
		assertThat(avisos()).isEqualTo(avisosAntes + 2);
		List<String> perfis = jdbc.queryForList(
				"select perfil_destino from notificacao where tipo = 'VAGA_ABERTA' order by criada_em desc limit 2",
				String.class);
		assertThat(perfis).containsExactlyInAnyOrder("COORDENADOR", "ADMINISTRATIVO");
		assertThat(jdbc.queryForObject(
				"select mensagem from notificacao where tipo = 'VAGA_ABERTA' order by criada_em desc limit 1",
				String.class))
			.startsWith("Uma vaga foi aberta no serviço (1 vaga disponível).")
			.doesNotContain("Fictícia");
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "vaga-desligamento-sobra")
	void desligarComVagaJaAbertaNaoAvisaDeNovo() throws Exception {
		AtendidosDeTeste atendidos = new AtendidosDeTeste(mvc, jdbc);
		String insercao = atendidos.inserirNoServico("Pessoa Fictícia " + UUID.randomUUID(), null, "TEA", "MATUTINO");
		definirCapacidade(numero("$.ocupadas") + 1);
		long avisosAntes = avisos();

		atendidos.criar("/api/atendidos/" + insercao + "/desligamento",
				Map.of("data", AtendidosDeTeste.HOJE.toString(), "motivo", "EVASAO"));

		assertThat(avisos()).isEqualTo(avisosAntes);
	}

	private long avisos() {
		return jdbc.queryForObject("select count(*) from notificacao where tipo = 'VAGA_ABERTA'", Long.class);
	}

	private long numero(String caminho) throws Exception {
		String corpo = mvc.perform(get("/api/lista-espera/vagas")).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(corpo, caminho)).longValue();
	}

	private void definirCapacidade(long vagas) throws Exception {
		mvc.perform(put("/api/lista-espera/capacidade").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"vagas\": " + vagas + "}")).andExpect(status().isOk());
	}

}
