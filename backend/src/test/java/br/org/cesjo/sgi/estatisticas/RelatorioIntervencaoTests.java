package br.org.cesjo.sgi.estatisticas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
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
 * Relatório de intervenção (CREAS) (issue #60): casos de violação de direitos do período com os
 * atendimentos e a situação de cada usuário, resumo com os encaminhamentos, leitura e exportação
 * auditadas e acesso só para quem lê relatórios e violações. O banco é compartilhado, então o teste
 * procura os próprios usuários pelo nome e compara o resumo antes e depois. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RelatorioIntervencaoTests {

	private static final String URL = "/api/estatisticas/relatorios/intervencao";

	private static final String INICIO = "2002-05-01";

	private static final String FIM = "2002-05-31";

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
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-intervencao-coord")
	void listaOsCasosDoPeriodoComAsIntervencoes() throws Exception {
		DadosDeTeste dados = new DadosDeTeste(mvc, jdbc);
		String antes = ler();

		String ana = dados.inserirNoServico("Intervenção Ana Fictícia", null, "TEA", "MATUTINO", dia(4, 20));
		dados.registrarViolacao(ana, "NEGLIGENCIA_ABANDONO", null, dia(5, 3));
		dados.registrarViolacao(ana, "VIOLENCIA_FISICA_PSICOLOGICA", "INTRAFAMILIAR", dia(5, 10));
		dados.registrarViolacao(ana, "ISOLAMENTO_SOCIAL", null, dia(6, 2));
		String bruno = dados.inserirNoServico("Intervenção Bruno Fictício", null, "TEA", "VESPERTINO", dia(4, 22));
		dados.registrarViolacao(bruno, "CONFINAMENTO", null, dia(5, 8));
		UUID psicologa = dados.colaborador("Psicóloga Fictícia da Intervenção");
		dados.registrarAtividade("ESTUDO_DE_CASO", dia(5, 12), ana, psicologa);
		dados.registrarAtividade("ATENDIMENTO_TECNICO_INDIVIDUALIZADO", dia(5, 20), ana, psicologa);
		dados.registrarAtividade("COM_USUARIOS", dia(5, 15), ana, psicologa);
		dados.registrarAtividade("ATENDIMENTO_TECNICO_INDIVIDUALIZADO", dia(6, 3), ana, psicologa);
		dados.desligar(bruno, dia(5, 25), "EVASAO");
		dados.registrarComunicacao("ENCAMINHAMENTO", "ENVIADA", dia(5, 9));
		reset(auditoria);

		String depois = ler();
		assertThat(JsonPath.<String>read(depois, "$.titulo"))
			.isEqualTo("Relatório de intervenção de 01/05/2002 a 31/05/2002");

		Map<String, Object> casoAna = caso(depois, "Intervenção Ana Fictícia");
		assertThat(JsonPath.<List<String>>read(casoAna, "$.violacoes[*].categoria"))
			.containsExactly("Negligência ou abandono", "Violência física ou psicológica");
		assertThat(JsonPath.<List<String>>read(casoAna, "$.violacoes[*].ambito")).containsExactly(null, "Intrafamiliar");
		assertThat(JsonPath.<Integer>read(casoAna, "$.atendimentos")).isEqualTo(2);
		assertThat(JsonPath.<String>read(casoAna, "$.ultimoAtendimento")).isEqualTo("2002-05-20");
		assertThat(JsonPath.<String>read(casoAna, "$.situacao")).isEqualTo("Ativo no serviço");

		Map<String, Object> casoBruno = caso(depois, "Intervenção Bruno Fictício");
		assertThat(JsonPath.<Integer>read(casoBruno, "$.atendimentos")).isZero();
		assertThat(JsonPath.<Object>read(casoBruno, "$.ultimoAtendimento")).isNull();
		assertThat(JsonPath.<String>read(casoBruno, "$.situacao")).isEqualTo("Desligado em 25/05/2002: Evasão");

		assertThat(diferenca(antes, depois, "Casos", "Usuários com violação identificada")).isEqualTo(2);
		assertThat(diferenca(antes, depois, "Casos", "Violações identificadas")).isEqualTo(3);
		assertThat(diferenca(antes, depois, "Violações por categoria", "Confinamento")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Violações por categoria", "Isolamento social")).isZero();
		assertThat(diferenca(antes, depois, "Intervenções", "Atendimentos individuais aos usuários com violação"))
			.isEqualTo(2);
		assertThat(diferenca(antes, depois, "Intervenções", "Usuários com violação sem atendimento no período"))
			.isEqualTo(1);
		assertThat(diferenca(antes, depois, "Intervenções", "Encaminhamentos enviados (todos os usuários)"))
			.isEqualTo(1);
		assertThat(diferenca(antes, depois, "Intervenções", "Usuários com violação desligados até o fim do período"))
			.isEqualTo(1);
		assertThat(depois).doesNotContain("Descrição fictícia");

		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL
				&& e.entidade().equals("relatorio_intervencao") && (INICIO + "/" + FIM).equals(e.entidadeId())));
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.EXPORTAR));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-intervencao-planilha")
	void exportaAPlanilhaComOsCasosEAudita() throws Exception {
		DadosDeTeste dados = new DadosDeTeste(mvc, jdbc);
		String carla = dados.inserirNoServico("Intervenção Carla Fictícia", null, "TEA", "MATUTINO", dia(4, 1));
		dados.registrarViolacao(carla, "ABUSO_SEXUAL", "EXTRAFAMILIAR", dia(5, 6));
		reset(auditoria);

		byte[] planilha = mvc
			.perform(get(URL + "/exportacao").param("inicio", INICIO).param("fim", FIM).param("formato", "xlsx"))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		List<String> celulas = new ArrayList<>();
		try (XSSFWorkbook livro = new XSSFWorkbook(new ByteArrayInputStream(planilha))) {
			for (Row linha : livro.getSheetAt(0)) {
				for (Cell celula : linha) {
					celulas.add(celula.toString());
				}
			}
		}
		assertThat(celulas).contains("Relatório de intervenção de 01/05/2002 a 31/05/2002", "Usuário atendido",
				"Intervenção Carla Fictícia", "Abuso sexual, extrafamiliar (06/05/2002)", "Ativo no serviço",
				"Encaminhamentos enviados (todos os usuários)");
		verify(auditoria).registrar(
				argThat(e -> e.acao() == AcaoAuditoria.EXPORTAR && e.entidade().equals("relatorio_intervencao")));
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-intervencao-pdf")
	void exportaEmPdf() throws Exception {
		mvc.perform(get(URL + "/exportacao").param("inicio", INICIO).param("fim", FIM).param("formato", "pdf"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/pdf"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "relatorio-intervencao-periodo")
	void recusaPeriodoInvalido() throws Exception {
		String amanha = DadosDeTeste.HOJE.plusDays(1).toString();
		mvc.perform(get(URL).param("inicio", INICIO).param("fim", amanha)).andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL).param("inicio", FIM).param("fim", INICIO)).andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL).param("inicio", "2002-02-30").param("fim", FIM)).andExpect(status().isBadRequest());
		mvc.perform(get(URL + "/exportacao").param("inicio", INICIO).param("fim", FIM).param("formato", "doc"))
			.andExpect(status().isBadRequest());
		verify(auditoria, never()).registrar(argThat(e -> e.entidade().equals("relatorio_intervencao")));
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "relatorio-intervencao-consulta")
	void consultaNaoGeraPorqueNaoLeViolacoes() throws Exception {
		mvc.perform(get(URL).param("inicio", INICIO).param("fim", FIM)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/exportacao").param("inicio", INICIO).param("fim", FIM).param("formato", "pdf"))
			.andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "relatorio-intervencao-tecnica")
	void equipeTecnicaNaoGeraPorqueNaoLeRelatorios() throws Exception {
		mvc.perform(get(URL).param("inicio", INICIO).param("fim", FIM)).andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "relatorio-intervencao-admin")
	void administrativoNaoGera() throws Exception {
		mvc.perform(get(URL).param("inicio", INICIO).param("fim", FIM)).andExpect(status().isForbidden());
	}

	private String ler() throws Exception {
		return mvc.perform(get(URL).param("inicio", INICIO).param("fim", FIM))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private static Map<String, Object> caso(String corpo, String usuario) {
		List<Map<String, Object>> casos = JsonPath.read(corpo, "$.casos[?(@.usuario == '" + usuario + "')]");
		assertThat(casos).as("caso de %s", usuario).hasSize(1);
		return casos.get(0);
	}

	private static LocalDate dia(int mes, int dia) {
		return LocalDate.of(2002, mes, dia);
	}

	private static long quantidade(String corpo, String grupo, String indicador) {
		List<Number> valores = JsonPath.read(corpo,
				"$.resumo[?(@.grupo == '" + grupo + "' && @.indicador == '" + indicador + "')].quantidade");
		assertThat(valores).as("linha %s / %s", grupo, indicador).hasSize(1);
		return valores.get(0).longValue();
	}

	private static long diferenca(String antes, String depois, String grupo, String indicador) {
		return quantidade(depois, grupo, indicador) - quantidade(antes, grupo, indicador);
	}

}
