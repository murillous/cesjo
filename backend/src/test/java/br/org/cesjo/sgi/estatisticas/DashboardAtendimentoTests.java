package br.org.cesjo.sgi.estatisticas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
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
 * Dashboard de violações, atividades e desligamentos (issue #57). O banco é compartilhado e guarda
 * dados de outras execuções, então o teste compara as contagens antes e depois. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DashboardAtendimentoTests {

	private static final String URL = "/api/estatisticas/dashboards/atendimento";

	private static final LocalDate ANO = LocalDate.of(2003, 1, 1);

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "dashboard-atendimento-coord")
	void contaViolacoesAtividadesEDesligamentosDoPeriodo() throws Exception {
		DadosDeTeste dados = new DadosDeTeste(mvc, jdbc);
		String antes = ler();

		String ana = dados.inserirNoServico("Período Ana", null, "TEA", "MATUTINO", ANO.withDayOfMonth(10));
		String bruno = dados.inserirNoServico("Período Bruno", null, "TEA", "MATUTINO", ANO.withDayOfMonth(10));
		dados.desligar(bruno, LocalDate.of(2003, 3, 5), "EVASAO");
		String carla = dados.inserirNoServico("Período Carla", null, "TEA", "MATUTINO", LocalDate.of(2002, 6, 1));
		dados.desligar(carla, LocalDate.of(2002, 12, 1), "MUDANCA_MUNICIPIO");
		dados.inserirNoServico("Período Davi", null, "TEA", "MATUTINO", LocalDate.of(2003, 7, 1));

		dados.registrarViolacao(ana, "ABUSO_SEXUAL", "INTRAFAMILIAR", LocalDate.of(2003, 2, 1));
		dados.registrarViolacao(ana, "ISOLAMENTO_SOCIAL", null, LocalDate.of(2003, 8, 1));
		UUID psicologa = dados.colaborador("Psicóloga Fictícia do Período");
		dados.registrarAtividade("COM_USUARIOS", LocalDate.of(2003, 2, 10), ana, psicologa);
		dados.registrarAtividade("ESTUDO_DE_CASO", LocalDate.of(2003, 4, 1), ana, psicologa);
		dados.registrarAtividade("ESTUDO_DE_CASO", LocalDate.of(2003, 9, 1), ana, psicologa);

		String depois = ler();
		assertThat(diferenca(antes, depois, "violacoesPorCategoria", "ABUSO_SEXUAL")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "violacoesPorCategoria", "ISOLAMENTO_SOCIAL")).isZero();
		assertThat(diferenca(antes, depois, "atividadesPorTipo", "COM_USUARIOS")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "atividadesPorTipo", "ESTUDO_DE_CASO")).isEqualTo(1);
		assertThat(numero(depois, "$.totalAtividades") - numero(antes, "$.totalAtividades")).isEqualTo(2);
		assertThat(numero(depois, "$.atendidosNoPeriodo") - numero(antes, "$.atendidosNoPeriodo")).isEqualTo(2);
		assertThat(numero(depois, "$.totalDesligamentos") - numero(antes, "$.totalDesligamentos")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "desligamentosPorMotivo", "EVASAO")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "desligamentosPorMotivo", "MUDANCA_MUNICIPIO")).isZero();

		// A taxa é desligamentos sobre atendidos no período, geral e por motivo.
		double atendidos = numero(depois, "$.atendidosNoPeriodo");
		assertThat((Double) JsonPath.read(depois, "$.taxaDesligamento"))
			.isCloseTo(numero(depois, "$.totalDesligamentos") / atendidos, within(1e-9));
		assertThat(JsonPath.<List<Double>>read(depois, "$.desligamentosPorMotivo[?(@.chave == 'EVASAO')].taxa").get(0))
			.isCloseTo(DadosDeTeste.total(depois, "desligamentosPorMotivo", "EVASAO") / atendidos, within(1e-9));

		mvc.perform(get(URL).param("inicio", "2003-01-01").param("fim", "2003-06-30"))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.inicio").value("2003-01-01"))
			.andExpect(jsonPath("$.violacoesPorCategoria.length()").value(12))
			.andExpect(jsonPath("$.atividadesPorTipo.length()").value(6))
			.andExpect(jsonPath("$.atividadesPorTipo[?(@.chave == 'ESTUDO_DE_CASO')].rotulo").value("Estudo de caso"))
			.andExpect(jsonPath("$.desligamentosPorMotivo.length()").value(5));
	}

	private String ler() throws Exception {
		return mvc.perform(get(URL).param("inicio", "2003-01-01").param("fim", "2003-06-30"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private static long numero(String corpo, String caminho) {
		return ((Number) JsonPath.read(corpo, caminho)).longValue();
	}

	private static long diferenca(String antes, String depois, String grafico, String chave) {
		return DadosDeTeste.total(depois, grafico, chave) - DadosDeTeste.total(antes, grafico, chave);
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "dashboard-atendimento-consulta")
	void consultaVeSemAsViolacoes() throws Exception {
		mvc.perform(get(URL).param("inicio", "2003-01-01").param("fim", "2003-06-30"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.violacoesPorCategoria").value(Matchers.nullValue()))
			.andExpect(jsonPath("$.atividadesPorTipo").isArray());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "dashboard-atendimento-periodo")
	void recusaPeriodoInvertidoOuIncompleto() throws Exception {
		mvc.perform(get(URL).param("inicio", "2003-06-30").param("fim", "2003-01-01"))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL).param("inicio", "2003-06-30")).andExpect(status().isBadRequest());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "dashboard-atendimento-tecnica")
	void equipeTecnicaNaoVe() throws Exception {
		mvc.perform(get(URL).param("inicio", "2003-01-01").param("fim", "2003-06-30"))
			.andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "dashboard-atendimento-admin")
	void administrativoNaoVe() throws Exception {
		mvc.perform(get(URL).param("inicio", "2003-01-01").param("fim", "2003-06-30"))
			.andExpect(status().isForbidden());
	}

}
