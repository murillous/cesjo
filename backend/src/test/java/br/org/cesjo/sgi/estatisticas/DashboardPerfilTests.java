package br.org.cesjo.sgi.estatisticas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;

/**
 * Dashboard de perfil dos usuários atendidos (issue #56): contagens por deficiência, gênero, faixa
 * etária e turno, recalculadas a cada acesso, só para quem lê relatórios. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DashboardPerfilTests {

	private static final String URL = "/api/estatisticas/dashboards/perfil";

	private static final LocalDate HOJE = DadosDeTeste.HOJE;

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "dashboard-perfil-coord")
	void contaOsAtivosPorCategoriaACadaAcesso() throws Exception {
		DadosDeTeste dados = new DadosDeTeste(mvc, jdbc);
		String antes = ler();

		String bia = dados.inserirNoServico("Perfil Bia", HOJE.minusYears(20), "T21", "INTEGRAL", HOJE.minusDays(1));
		dados.informarGenero(bia, "Perfil Bia", HOJE.minusYears(20), "T21", "FEMININO");
		dados.inserirNoServico("Perfil Caio", HOJE.minusYears(40), "FISICA", "VESPERTINO", HOJE.minusDays(1));
		dados.inserirNoServico("Perfil Dora", null, "FISICA", "VESPERTINO", HOJE.minusDays(1));

		String depois = ler();
		assertThat(numero(depois, "$.totalAtivos") - numero(antes, "$.totalAtivos")).isEqualTo(3);
		assertThat(diferenca(antes, depois, "porTipoDeficiencia", "T21")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "porTipoDeficiencia", "FISICA")).isEqualTo(2);
		assertThat(diferenca(antes, depois, "porGenero", "FEMININO")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "porGenero", "SEM_VALOR")).isEqualTo(2);
		assertThat(diferenca(antes, depois, "porFaixaEtaria", "JOVEM")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "porFaixaEtaria", "ADULTO")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "porFaixaEtaria", "SEM_VALOR")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "porTurno", "INTEGRAL")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "porTurno", "VESPERTINO")).isEqualTo(2);

		mvc.perform(get(URL))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.porTurno.length()").value(3))
			.andExpect(jsonPath("$.porTurno[0].chave").value("MATUTINO"))
			.andExpect(jsonPath("$.porTurno[0].rotulo").value("Matutino"))
			.andExpect(jsonPath("$.geradoEm").isNotEmpty());
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "dashboard-perfil-consulta")
	void consultaVeODashboard() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isOk());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "dashboard-perfil-tecnica")
	void equipeTecnicaNaoVe() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "dashboard-perfil-admin")
	void administrativoNaoVe() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isForbidden());
	}

	private String ler() throws Exception {
		return mvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}

	private static long numero(String corpo, String caminho) {
		return ((Number) JsonPath.read(corpo, caminho)).longValue();
	}

	private static long diferenca(String antes, String depois, String grafico, String chave) {
		return DadosDeTeste.total(depois, grafico, chave) - DadosDeTeste.total(antes, grafico, chave);
	}

}
