package br.org.cesjo.sgi.estatisticas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;

/**
 * Relatórios estatísticos quadrimestral e anual (issue #59): o mesmo conteúdo do mensal agregado no
 * período, exportação em PDF e Excel auditada. O banco é compartilhado, então o teste compara as linhas
 * antes e depois. Usa o ano de 2003, que nenhum outro teste usa. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RelatorioQuadrimestralEAnualTests {

	private static final String URL = "/api/estatisticas/relatorios";

	private static final String ANO = "2003";

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
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-periodo-coord")
	void agregaOMesmoConteudoDoMensalPorQuadrimestreEAno() throws Exception {
		DadosDeTeste dados = new DadosDeTeste(mvc, jdbc);
		String primeiroAntes = ler(quadrimestre(1));
		String segundoAntes = ler(quadrimestre(2));
		String anoAntes = ler(anual());

		// Abril fica no 1º quadrimestre; maio e setembro ficam no 2º e no 3º.
		String lia = dados.inserirNoServico("Periodo Lia", null, "TEA", "MATUTINO", LocalDate.of(2003, 4, 10));
		dados.inscrever("Periodo Ivo", null, "TEA", LocalDate.of(2003, 5, 2));
		UUID psicologo = dados.colaborador("Psicólogo Fictício do Período");
		dados.registrarAtividade("ESTUDO_DE_CASO", LocalDate.of(2003, 4, 30), lia, psicologo);
		dados.registrarAtividade("ESTUDO_DE_CASO", LocalDate.of(2003, 9, 1), lia, psicologo);
		dados.desligar(lia, LocalDate.of(2003, 12, 31), "EVASAO");

		String primeiro = ler(quadrimestre(1));
		assertThat(JsonPath.<String>read(primeiro, "$.titulo"))
			.isEqualTo("Relatório quadrimestral de janeiro a abril de 2003");
		assertThat(JsonPath.<String>read(primeiro, "$.inicio")).isEqualTo("2003-01-01");
		assertThat(JsonPath.<String>read(primeiro, "$.fim")).isEqualTo("2003-04-30");
		assertThat(diferenca(primeiroAntes, primeiro, "Usuários atendidos", "Inserções no serviço")).isEqualTo(1);
		assertThat(diferenca(primeiroAntes, primeiro, "Atendimentos", "Estudo de caso")).isEqualTo(1);
		assertThat(diferenca(primeiroAntes, primeiro, "Lista de espera", "Novas inscrições")).isEqualTo(1);

		String segundo = ler(quadrimestre(2));
		assertThat(JsonPath.<String>read(segundo, "$.inicio")).isEqualTo("2003-05-01");
		assertThat(JsonPath.<String>read(segundo, "$.fim")).isEqualTo("2003-08-31");
		assertThat(diferenca(segundoAntes, segundo, "Lista de espera", "Novas inscrições")).isEqualTo(1);
		assertThat(diferenca(segundoAntes, segundo, "Atendimentos", "Estudo de caso")).isZero();
		assertThat(diferenca(segundoAntes, segundo, "Usuários atendidos", "Usuários ativos no último dia"))
			.isEqualTo(1);

		String ano = ler(anual());
		assertThat(JsonPath.<String>read(ano, "$.titulo")).isEqualTo("Relatório anual de 2003");
		assertThat(JsonPath.<String>read(ano, "$.inicio")).isEqualTo("2003-01-01");
		assertThat(JsonPath.<String>read(ano, "$.fim")).isEqualTo("2003-12-31");
		assertThat(diferenca(anoAntes, ano, "Lista de espera", "Novas inscrições")).isEqualTo(2);
		assertThat(diferenca(anoAntes, ano, "Atendimentos", "Estudo de caso")).isEqualTo(2);
		assertThat(diferenca(anoAntes, ano, "Desligamentos", "Evasão")).isEqualTo(1);
		assertThat(diferenca(anoAntes, ano, "Usuários atendidos", "Usuários ativos no último dia")).isZero();

		List<String> indicadoresDoMensal = JsonPath
			.read(ler(get(URL + "/mensal").param("mes", "2003-01")), "$.linhas[*].indicador");
		assertThat(JsonPath.<List<String>>read(ano, "$.linhas[*].indicador")).isEqualTo(indicadoresDoMensal);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-periodo-exporta")
	void exportaEmPdfEExcelEAudita() throws Exception {
		mvc.perform(get(URL + "/quadrimestral/exportacao").param("ano", ANO)
			.param("quadrimestre", "3")
			.param("formato", "xlsx"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition",
					Matchers.containsString("relatorio-quadrimestral-de-setembro-a-dezembro-de-2003")));
		mvc.perform(get(URL + "/anual/exportacao").param("ano", ANO).param("formato", "pdf"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/pdf"))
			.andExpect(header().string("Content-Disposition", Matchers.containsString("relatorio-anual-de-2003")));
		verify(auditoria, org.mockito.Mockito.times(2)).registrar(
				argThat(e -> e.acao() == AcaoAuditoria.EXPORTAR && e.entidade().equals("relatorio_estatistico")));
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "relatorio-periodo-consulta")
	void consultaGeraEExporta() throws Exception {
		mvc.perform(quadrimestre(1)).andExpect(status().isOk());
		mvc.perform(get(URL + "/anual/exportacao").param("ano", ANO).param("formato", "xlsx"))
			.andExpect(status().isOk());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-periodo-invalido")
	void recusaPeriodoFuturoEInvalido() throws Exception {
		String futuro = Year.now().plusYears(1).toString();
		mvc.perform(get(URL + "/anual").param("ano", futuro)).andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL + "/quadrimestral").param("ano", futuro).param("quadrimestre", "1"))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL + "/quadrimestral").param("ano", ANO).param("quadrimestre", "4"))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL + "/quadrimestral").param("ano", ANO).param("quadrimestre", "x"))
			.andExpect(status().isBadRequest());
		mvc.perform(get(URL + "/anual").param("ano", "20x3")).andExpect(status().isBadRequest());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-periodo-corrente")
	void anoCorrenteSaiParcial() throws Exception {
		String corrente = ler(get(URL + "/anual").param("ano", String.valueOf(DadosDeTeste.HOJE.getYear())));
		assertThat(JsonPath.<String>read(corrente, "$.fim")).endsWith("-12-31");
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "relatorio-periodo-tecnica")
	void equipeTecnicaNaoGera() throws Exception {
		mvc.perform(quadrimestre(1)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/anual/exportacao").param("ano", ANO).param("formato", "pdf"))
			.andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "relatorio-periodo-admin")
	void administrativoNaoGera() throws Exception {
		mvc.perform(anual()).andExpect(status().isForbidden());
	}

	private static MockHttpServletRequestBuilder quadrimestre(int numero) {
		return get(URL + "/quadrimestral").param("ano", ANO).param("quadrimestre", String.valueOf(numero));
	}

	private static MockHttpServletRequestBuilder anual() {
		return get(URL + "/anual").param("ano", ANO);
	}

	private String ler(MockHttpServletRequestBuilder pedido) throws Exception {
		return mvc.perform(pedido)
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private static long quantidade(String corpo, String grupo, String indicador) {
		List<Number> valores = JsonPath.read(corpo,
				"$.linhas[?(@.grupo == '" + grupo + "' && @.indicador == '" + indicador + "')].quantidade");
		assertThat(valores).as("linha %s / %s", grupo, indicador).hasSize(1);
		return valores.get(0).longValue();
	}

	private static long diferenca(String antes, String depois, String grupo, String indicador) {
		return quantidade(depois, grupo, indicador) - quantidade(antes, grupo, indicador);
	}

}
