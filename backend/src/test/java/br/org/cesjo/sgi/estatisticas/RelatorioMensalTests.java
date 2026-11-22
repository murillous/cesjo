package br.org.cesjo.sgi.estatisticas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
 * Relatório estatístico mensal (issue #58): escolha do mês, quantitativos de cada área pelas APIs dos
 * módulos, exportação em PDF e Excel auditada. O banco é compartilhado e guarda dados de outras
 * execuções, então o teste compara as linhas antes e depois. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RelatorioMensalTests {

	private static final String URL = "/api/estatisticas/relatorios/mensal";

	private static final String MES = "2004-03";

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
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-mensal-coord")
	void consolidaOsQuantitativosDoMes() throws Exception {
		DadosDeTeste dados = new DadosDeTeste(mvc, jdbc);
		String antes = ler(MES);

		dados.inscrever("Mensal Ivo", null, "TEA", dia(5));
		String jade = JsonPath.read(dados.inscrever("Mensal Jade", null, "TEA", dia(6)), "$.id");
		dados.mudarSituacao(jade, "SAIU", dia(20), "DESISTENCIA");
		String lia = dados.inserirNoServico("Mensal Lia", null, "TEA", "MATUTINO", dia(15));
		String mario = dados.inserirNoServico("Mensal Mário", null, "TEA", "MATUTINO", dia(12));
		dados.desligar(mario, dia(25), "EVASAO");
		UUID psicologo = dados.colaborador("Psicólogo Fictício do Mês");
		dados.registrarAtividade("COM_USUARIOS", dia(16), lia, psicologo);
		dados.registrarAtividade("ESTUDO_DE_CASO", dia(17), lia, psicologo);
		dados.registrarComunicacao("OFICIO", "ENVIADA", dia(14));
		dados.registrarComunicacao("CONTATO_INSTITUCIONAL", null, dia(14));

		String depois = ler(MES);
		assertThat(JsonPath.<String>read(depois, "$.titulo")).isEqualTo("Relatório mensal de março de 2004");
		assertThat(JsonPath.<String>read(depois, "$.inicio")).isEqualTo("2004-03-01");
		assertThat(JsonPath.<String>read(depois, "$.fim")).isEqualTo("2004-03-31");
		assertThat(diferenca(antes, depois, "Lista de espera", "Novas inscrições")).isEqualTo(4);
		assertThat(diferenca(antes, depois, "Lista de espera", "Candidatos na fila no último dia")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Lista de espera", "Saídas da lista: Desistência")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Usuários atendidos", "Inserções no serviço")).isEqualTo(2);
		assertThat(diferenca(antes, depois, "Usuários atendidos", "Usuários atendidos no período")).isEqualTo(2);
		assertThat(diferenca(antes, depois, "Usuários atendidos", "Usuários ativos no último dia")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Atendimentos", "Estudo de caso")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Atividades", "Atividade com usuários")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Contatos e comunicações", "Ofícios expedidos")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Contatos e comunicações", "Ofícios recebidos")).isZero();
		assertThat(diferenca(antes, depois, "Contatos e comunicações", "Contatos telefônicos institucionais"))
			.isEqualTo(1);
		assertThat(diferenca(antes, depois, "Desligamentos", "Evasão")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Desligamentos", "Outros")).isZero();
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.EXPORTAR));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-mensal-planilha")
	void exportaAPlanilhaComAsMesmasLinhasEAudita() throws Exception {
		String relatorio = ler(MES);
		byte[] planilha = mvc.perform(get(URL + "/exportacao").param("mes", MES).param("formato", "xlsx"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition",
					org.hamcrest.Matchers.containsString("relatorio-mensal-de-marco-de-2004")))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		List<String> indicadores = new ArrayList<>();
		try (XSSFWorkbook livro = new XSSFWorkbook(new ByteArrayInputStream(planilha))) {
			XSSFSheet aba = livro.getSheetAt(0);
			for (Row linha : aba) {
				if (linha.getCell(1) != null) {
					indicadores.add(linha.getCell(1).toString());
				}
			}
			String textoTodo = indicadores.toString() + aba.getRow(0).getCell(0);
			assertThat(textoTodo).contains("Relatório mensal de março de 2004");
		}
		List<String> esperados = JsonPath.read(relatorio, "$.linhas[*].indicador");
		assertThat(indicadores).containsSubsequence(esperados);
		verify(auditoria).registrar(
				argThat(e -> e.acao() == AcaoAuditoria.EXPORTAR && e.entidade().equals("relatorio_estatistico")));
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "relatorio-mensal-consulta")
	void consultaGeraEExportaEmPdf() throws Exception {
		mvc.perform(get(URL).param("mes", MES)).andExpect(status().isOk());
		mvc.perform(get(URL + "/exportacao").param("mes", MES).param("formato", "pdf"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/pdf"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-mensal-futuro")
	void recusaMesFuturoEMesInvalido() throws Exception {
		String futuro = YearMonth.now().plusMonths(2).toString();
		mvc.perform(get(URL).param("mes", futuro)).andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL).param("mes", "2004-13")).andExpect(status().isBadRequest());
		mvc.perform(get(URL + "/exportacao").param("mes", MES).param("formato", "doc"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "relatorio-mensal-tecnica")
	void equipeTecnicaNaoGera() throws Exception {
		mvc.perform(get(URL).param("mes", MES)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/exportacao").param("mes", MES).param("formato", "pdf"))
			.andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "relatorio-mensal-admin")
	void administrativoNaoGera() throws Exception {
		mvc.perform(get(URL).param("mes", MES)).andExpect(status().isForbidden());
	}

	private String ler(String mes) throws Exception {
		return mvc.perform(get(URL).param("mes", mes))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.linhas").isArray())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private static LocalDate dia(int dia) {
		return LocalDate.of(2004, 3, dia);
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
