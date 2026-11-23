package br.org.cesjo.sgi.estatisticas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Relatório de transparência (issue #61): números agregados do período pelas APIs dos módulos, sem
 * nenhum dado pessoal, e exportação em PDF e Excel auditada. O banco é compartilhado e guarda dados de
 * outras execuções, então o teste compara as linhas antes e depois. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RelatorioTransparenciaTests {

	private static final String URL = "/api/estatisticas/relatorios/transparencia";

	private static final String INICIO = "2002-01-01";

	private static final String FIM = "2002-12-31";

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
	@ComConta(perfil = Perfil.COORDENADOR, login = "transparencia-coord")
	void consolidaOsNumerosDoPeriodoSemDadoPessoal() throws Exception {
		DadosDeTeste dados = new DadosDeTeste(mvc, jdbc);
		String antes = ler(INICIO, FIM);

		dados.inscrever("Transparência Rui", null, "TEA", dia(3, 5));
		String lia = dados.inserirNoServico("Transparência Lia", null, "TEA", "MATUTINO", dia(4, 15));
		UUID psicologo = dados.colaborador("Psicóloga Fictícia da Transparência");
		dados.registrarAtividade("COM_FAMILIAS", dia(5, 16), lia, psicologo);
		String parceiro = parceiro("Parceiro Fictício Transparência", "2001-06-01", "2002-03-31");
		parceiro("Parceiro Fictício Vencido", "2000-01-01", "2001-12-31");
		articulacao(parceiro, dia(2, 10));
		evento("FORMACAO_MULTIPROFISSIONAL", null, dia(6, 1));
		evento("EVENTO", "REALIZADO", dia(7, 1));
		doacao("RECEBIDA", "DINHEIRO", "1500.50", dia(8, 1));
		doacao("RECEBIDA", "DINHEIRO", "250.00", dia(8, 2));
		doacao("RECEBIDA", "ALIMENTOS", null, dia(8, 3));
		doacao("REALIZADA", "VESTUARIO", null, dia(9, 3));
		doacao("RECEBIDA", "ALIMENTOS", null, LocalDate.of(2003, 1, 2));
		colaboradorComVinculo("TERAPEUTA_OCUPACIONAL", dia(2, 1), null);
		colaboradorComVinculo("ASSISTENTE_SOCIAL", LocalDate.of(2001, 1, 1), dia(6, 30));

		String depois = ler(INICIO, FIM);
		assertThat(JsonPath.<String>read(depois, "$.titulo")).isEqualTo("Relatório de transparência de 2002");
		assertThat(diferenca(antes, depois, "Usuários atendidos", "Usuários atendidos no período")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Usuários atendidos", "Inserções no serviço")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Lista de espera", "Novas inscrições")).isEqualTo(2);
		assertThat(diferenca(antes, depois, "Atendimentos e atividades", "Atividade com famílias")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Parcerias", "Parcerias vigentes no período")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Parcerias", "Ações de articulação com parceiros")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Reuniões e eventos institucionais", "Formação multiprofissional"))
			.isEqualTo(1);
		assertThat(diferenca(antes, depois, "Reuniões e eventos institucionais", "Evento")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Doações recebidas", "Dinheiro")).isEqualTo(2);
		assertThat(diferenca(antes, depois, "Doações recebidas", "Alimentos")).isEqualTo(1);
		assertThat(valor(depois, "Doações recebidas", "Valor recebido em dinheiro")
			.subtract(valor(antes, "Doações recebidas", "Valor recebido em dinheiro"))).isEqualByComparingTo("1750.50");
		assertThat(unidade(depois, "Doações recebidas", "Valor recebido em dinheiro")).isEqualTo("REAIS");
		assertThat(diferenca(antes, depois, "Doações feitas pelo Centro-Dia", "Doações feitas")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Equipe no último dia", "Terapeuta ocupacional")).isEqualTo(1);
		assertThat(diferenca(antes, depois, "Equipe no último dia", "Assistente social")).isZero();
		// A psicóloga sem vínculo registrado conta pela situação de hoje (ativa).
		assertThat(diferenca(antes, depois, "Equipe no último dia", "Psicólogo(a)")).isEqualTo(1);

		assertThat(depois).doesNotContain("Transparência Rui", "Transparência Lia", "Psicóloga Fictícia",
				"Parceiro Fictício", "Doador Fictício", "Descrição fictícia");
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.EXPORTAR));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "transparencia-periodo")
	void periodoQueNaoEAnoInteiroLevaAsDatasNoTitulo() throws Exception {
		String corpo = ler("2002-01-01", "2002-06-30");
		assertThat(JsonPath.<String>read(corpo, "$.titulo"))
			.isEqualTo("Relatório de transparência de 01/01/2002 a 30/06/2002");
		assertThat(JsonPath.<String>read(corpo, "$.inicio")).isEqualTo("2002-01-01");
		assertThat(JsonPath.<String>read(corpo, "$.fim")).isEqualTo("2002-06-30");
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "transparencia-planilha")
	void exportaAPlanilhaComAsMesmasLinhasEAudita() throws Exception {
		String relatorio = ler(INICIO, FIM);
		byte[] planilha = mvc
			.perform(get(URL + "/exportacao").param("inicio", INICIO).param("fim", FIM).param("formato", "xlsx"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition",
					org.hamcrest.Matchers.containsString("relatorio-de-transparencia-de-2002")))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		List<String> indicadores = new ArrayList<>();
		String valorEmDinheiro = null;
		try (XSSFWorkbook livro = new XSSFWorkbook(new ByteArrayInputStream(planilha))) {
			XSSFSheet aba = livro.getSheetAt(0);
			assertThat(aba.getRow(0).getCell(0).toString()).contains("Relatório de transparência de 2002");
			for (Row linha : aba) {
				if (linha.getCell(1) != null) {
					indicadores.add(linha.getCell(1).toString());
					if ("Valor recebido em dinheiro".equals(linha.getCell(1).toString())) {
						valorEmDinheiro = linha.getCell(2).toString();
					}
				}
			}
		}
		List<String> esperados = JsonPath.read(relatorio, "$.linhas[*].indicador");
		assertThat(indicadores).containsSubsequence(esperados);
		assertThat(valorEmDinheiro).startsWith("R$ ").contains(",");
		verify(auditoria).registrar(
				argThat(e -> e.acao() == AcaoAuditoria.EXPORTAR && e.entidade().equals("relatorio_transparencia")));
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "transparencia-consulta")
	void consultaGeraEExportaEmPdf() throws Exception {
		mvc.perform(get(URL).param("inicio", INICIO).param("fim", FIM)).andExpect(status().isOk());
		mvc.perform(get(URL + "/exportacao").param("inicio", INICIO).param("fim", FIM).param("formato", "pdf"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/pdf"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "transparencia-invalido")
	void recusaPeriodoFuturoInvertidoOuInvalido() throws Exception {
		String amanha = DadosDeTeste.HOJE.plusDays(1).toString();
		mvc.perform(get(URL).param("inicio", INICIO).param("fim", amanha)).andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL).param("inicio", FIM).param("fim", INICIO)).andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL).param("inicio", "2002-02-30").param("fim", FIM)).andExpect(status().isBadRequest());
		mvc.perform(get(URL + "/exportacao").param("inicio", INICIO).param("fim", FIM).param("formato", "doc"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "transparencia-tecnica")
	void equipeTecnicaNaoGera() throws Exception {
		mvc.perform(get(URL).param("inicio", INICIO).param("fim", FIM)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/exportacao").param("inicio", INICIO).param("fim", FIM).param("formato", "pdf"))
			.andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "transparencia-admin")
	void administrativoNaoGera() throws Exception {
		mvc.perform(get(URL).param("inicio", INICIO).param("fim", FIM)).andExpect(status().isForbidden());
	}

	private String parceiro(String nome, String vigenciaInicio, String vigenciaFim) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("nome", nome);
		dados.put("tipo", "EMPRESA");
		dados.put("instrumento", null);
		dados.put("vigenciaInicio", vigenciaInicio);
		dados.put("vigenciaFim", vigenciaFim);
		dados.put("contato", null);
		dados.put("observacoes", null);
		dados.put("ativo", true);
		return JsonPath.read(new DadosDeTeste(mvc, jdbc).enviar(post("/api/parceiros"), dados, 201), "$.id");
	}

	private void articulacao(String parceiroId, LocalDate data) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("data", data.toString());
		dados.put("descricao", "Descrição fictícia.");
		new DadosDeTeste(mvc, jdbc).enviar(post("/api/parceiros/{id}/articulacoes", parceiroId), dados, 201);
	}

	private void evento(String tipo, String direcao, LocalDate data) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("tipo", tipo);
		dados.put("direcao", direcao);
		dados.put("data", data.toString());
		dados.put("descricao", "Descrição fictícia.");
		dados.put("participantes", "Participante Fictício");
		dados.put("parceiroId", null);
		new DadosDeTeste(mvc, jdbc).enviar(post("/api/eventos-institucionais"), dados, 201);
	}

	private void doacao(String direcao, String tipo, String valor, LocalDate data) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("direcao", direcao);
		dados.put("data", data.toString());
		dados.put("contraparte", "Doador Fictício");
		dados.put("tipo", tipo);
		dados.put("descricao", "Descrição fictícia.");
		dados.put("quantidade", valor == null ? "3 caixas" : null);
		dados.put("valor", valor == null ? null : new BigDecimal(valor));
		new DadosDeTeste(mvc, jdbc).enviar(post("/api/doacoes"), dados, 201);
	}

	/** Colaborador com um vínculo, gravado direto no banco; com desligamento, fica inativo. */
	private void colaboradorComVinculo(String funcao, LocalDate contratacao, LocalDate desligamento) {
		UUID id = UUID.randomUUID();
		Timestamp agora = Timestamp.from(Instant.now());
		jdbc.update("""
				insert into colaborador (id, nome, funcao, ativo, criado_em, atualizado_em)
				values (?, 'Colaborador Fictício', ?, ?, ?, ?)""", id, funcao, desligamento == null, agora, agora);
		jdbc.update("""
				insert into vinculo_colaborador (id, colaborador_id, data_contratacao, data_desligamento,
				    motivo_desligamento, criado_em, atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?)""", UUID.randomUUID(), id, Date.valueOf(contratacao),
				desligamento == null ? null : Date.valueOf(desligamento), desligamento == null ? null : "v1:ficticio",
				agora, agora);
	}

	private String ler(String inicio, String fim) throws Exception {
		return mvc.perform(get(URL).param("inicio", inicio).param("fim", fim))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.linhas").isArray())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private static LocalDate dia(int mes, int dia) {
		return LocalDate.of(2002, mes, dia);
	}

	private static BigDecimal valor(String corpo, String grupo, String indicador) {
		List<Number> valores = JsonPath.read(corpo,
				"$.linhas[?(@.grupo == '" + grupo + "' && @.indicador == '" + indicador + "')].valor");
		assertThat(valores).as("linha %s / %s", grupo, indicador).hasSize(1);
		return new BigDecimal(valores.get(0).toString());
	}

	private static String unidade(String corpo, String grupo, String indicador) {
		List<String> unidades = JsonPath.read(corpo,
				"$.linhas[?(@.grupo == '" + grupo + "' && @.indicador == '" + indicador + "')].unidade");
		return unidades.get(0);
	}

	private static long diferenca(String antes, String depois, String grupo, String indicador) {
		return valor(depois, grupo, indicador).subtract(valor(antes, grupo, indicador)).longValueExact();
	}

}
