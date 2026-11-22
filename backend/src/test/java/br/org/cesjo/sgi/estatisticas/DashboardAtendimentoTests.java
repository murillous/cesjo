package br.org.cesjo.sgi.estatisticas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

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
 * Dashboard de violações, atividades e desligamentos (issue #57). Os dados ficam em 2003, um ano que
 * nenhum outro teste usa, então as contagens do período são exatas mesmo com o banco compartilhado.
 * Só dados fictícios.
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

		mvc.perform(get(URL).param("inicio", "2003-01-01").param("fim", "2003-06-30"))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.inicio").value("2003-01-01"))
			.andExpect(jsonPath("$.violacoesPorCategoria.length()").value(12))
			.andExpect(jsonPath("$.violacoesPorCategoria[?(@.chave == 'ABUSO_SEXUAL')].total").value(1))
			.andExpect(jsonPath("$.violacoesPorCategoria[?(@.chave == 'ISOLAMENTO_SOCIAL')].total").value(0))
			.andExpect(jsonPath("$.atividadesPorTipo.length()").value(6))
			.andExpect(jsonPath("$.atividadesPorTipo[?(@.chave == 'COM_USUARIOS')].total").value(1))
			.andExpect(jsonPath("$.atividadesPorTipo[?(@.chave == 'ESTUDO_DE_CASO')].rotulo").value("Estudo de caso"))
			.andExpect(jsonPath("$.atividadesPorTipo[?(@.chave == 'ESTUDO_DE_CASO')].total").value(1))
			.andExpect(jsonPath("$.totalAtividades").value(2))
			.andExpect(jsonPath("$.atendidosNoPeriodo").value(2))
			.andExpect(jsonPath("$.totalDesligamentos").value(1))
			.andExpect(jsonPath("$.taxaDesligamento").value(0.5))
			.andExpect(jsonPath("$.desligamentosPorMotivo[?(@.chave == 'EVASAO')].total").value(1))
			.andExpect(jsonPath("$.desligamentosPorMotivo[?(@.chave == 'EVASAO')].taxa").value(0.5))
			.andExpect(jsonPath("$.desligamentosPorMotivo[?(@.chave == 'MUDANCA_MUNICIPIO')].total").value(0));
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
