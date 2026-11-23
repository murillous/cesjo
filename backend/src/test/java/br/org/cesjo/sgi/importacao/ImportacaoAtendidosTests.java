package br.org.cesjo.sgi.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;

/** Importação dos usuários atendidos atuais de planilha Excel (issue #62, ADR 0220). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ImportacaoAtendidosTests {

	private static final String URL = "/api/importacao/atendidos";

	private static final List<String> CABECALHO = List.of("Nome completo", "Data de nascimento", "Tipo de deficiência",
			"Telefone", "Nome do cuidador responsável", "Origem do encaminhamento", "Data de cadastro na lista de espera",
			"Data de inserção", "Turno");

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	Auditoria auditoria;

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "importacao-atendidos-modelo")
	void modeloTemAsColunasDaInsercao() throws Exception {
		byte[] modelo = mvc.perform(get(URL + "/modelo"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		try (XSSFWorkbook livro = new XSSFWorkbook(new ByteArrayInputStream(modelo))) {
			DataFormatter formato = new DataFormatter();
			List<String> titulos = new ArrayList<>();
			livro.getSheet("Dados").getRow(0).forEach(celula -> titulos.add(formato.formatCellValue(celula)));
			assertThat(titulos).contains("Data de inserção *", "Turno *", "Data de cadastro na lista de espera");
			StringBuilder instrucoes = new StringBuilder();
			livro.getSheet("Instruções").forEach(linha -> linha.forEach(c -> instrucoes.append(formato.formatCellValue(c)).append('|')));
			assertThat(instrucoes).contains("Matutino; Vespertino; Integral");
		}
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "importacao-atendidos-previa")
	void previaMostraErrosDaInsercaoPorLinha() throws Exception {
		inscrever("Davi Previatendido");
		byte[] planilha = Planilhas.xlsx(CABECALHO, List.of(
				linha("Ana Previatendido", "15/03/1990", "TEA", "3132345678", "Rosa", "CRAS", "02/01/2020", "01/03/2020", "Matutino"),
				linha("Bia Previatendido", null, "TEA", "3132345678", "Rosa", "CRAS", "02/01/2020", "01/01/2999", "Noturno"),
				linha("Caio Previatendido", null, "TEA", "3132345678", "Rosa", "CRAS", "02/01/2020", "01/01/2019", "integral"),
				linha("Davi Previatendido", null, "TEA", "3132345678", "Rosa", "CRAS", null, "01/01/2020", "Integral")));

		enviar(URL + "/previa", planilha)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.validas").value(1))
			.andExpect(jsonPath("$.comErro").value(3))
			.andExpect(jsonPath("$.colunas[8]").value("Turno"))
			.andExpect(jsonPath("$.linhas[0].erros").isEmpty())
			.andExpect(jsonPath("$.linhas[1].erros", Matchers.hasItem("Data de inserção: não pode ser futura.")))
			.andExpect(jsonPath("$.linhas[1].erros", Matchers.hasItem(Matchers.startsWith("Turno: \"Noturno\""))))
			.andExpect(jsonPath("$.linhas[2].erros[0]").value(
					"Data de inserção: não pode ser anterior à data de cadastro na lista de espera."))
			.andExpect(jsonPath("$.linhas[3].erros[0]").value(
					"Esta pessoa já está na lista de espera: insira no serviço pela fila."));

		assertThat(jdbc.queryForObject("select count(*) from pessoa where nome_completo like '%Previatendido'",
				Integer.class)).isEqualTo(1);
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.IMPORTAR));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "importacao-atendidos-confirma")
	void confirmacaoCriaPessoaEInsercaoAtiva() throws Exception {
		byte[] planilha = Planilhas.xlsx(CABECALHO, List.of(
				linha("Eli Confirmatendido", LocalDate.of(1990, 5, 1), "Física", "(31) 98765-4321", "Marta",
						"Saúde", null, LocalDate.of(2021, 4, 5), "Vespertino"),
				linha("Fred Confirmatendido", null, "TEA", "3132345678", "Nina", "CRAS", "10/10/2019", "01/02/2020",
						"INTEGRAL"),
				linha("Gabi Confirmatendido", null, "TEA", "3132345678", null, "CRAS", null, "01/02/2020", "Integral")));

		enviar(URL, planilha)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.importadas").value(2))
			.andExpect(jsonPath("$.ignoradas").value(1));

		List<Map<String, Object>> gravados = jdbc.queryForList("select p.nome_completo, i.data_insercao, i.turno,"
				+ " i.situacao, r.nome_completo as cuidador, l.situacao as situacao_fila, l.data_cadastro"
				+ " from insercao i join pessoa p on p.id = i.pessoa_id"
				+ " join responsavel r on r.id = i.cuidador_responsavel_id"
				+ " join inscricao_lista_espera l on l.id = i.inscricao_id"
				+ " where p.nome_completo like '%Confirmatendido' order by p.nome_completo");
		assertThat(gravados).hasSize(2);
		assertThat(gravados.get(0)).containsEntry("nome_completo", "Eli Confirmatendido")
			.containsEntry("turno", "VESPERTINO")
			.containsEntry("situacao", "ATIVA")
			.containsEntry("cuidador", "Marta")
			.containsEntry("situacao_fila", "INSERIDO");
		assertThat(gravados.get(0).get("data_insercao").toString()).isEqualTo("2021-04-05");
		assertThat(gravados.get(0).get("data_cadastro").toString()).isEqualTo("2021-04-05");
		assertThat(gravados.get(1).get("data_cadastro").toString()).isEqualTo("2019-10-10");
		assertThat(gravados.get(1)).containsEntry("turno", "INTEGRAL");

		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.IMPORTAR
				&& e.entidade().equals("planilha_atendidos")));
		verify(auditoria, atLeastOnce()).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR
				&& e.entidade().equals("insercao")));

		// Quem já é usuário ativo não entra de novo.
		enviar(URL + "/previa", planilha)
			.andExpect(jsonPath("$.linhas[0].erros[0]").value("Já há um usuário atendido ativo com este nome."));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "importacao-atendidos-malformada")
	void planilhaSemColunaDaInsercaoERecusada() throws Exception {
		byte[] planilha = Planilhas.xlsx(CABECALHO.subList(0, 7),
				List.of(linha("Hugo Semcoluna", null, "TEA", "3132345678", "R", "CRAS", null)));
		enviar(URL + "/previa", planilha)
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value(Matchers.containsString("Data de inserção, Turno")));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "importacao-atendidos-administrativo")
	void administrativoNaoImporta() throws Exception {
		byte[] planilha = Planilhas.xlsx(CABECALHO, List.of(linha("Ivo Semacessoatendido", null, "TEA", "3132345678",
				"R", "CRAS", null, "01/02/2020", "Integral")));
		enviar(URL, planilha).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/modelo")).andExpect(status().isForbidden());
		assertThat(jdbc.queryForObject("select count(*) from pessoa where nome_completo = 'Ivo Semacessoatendido'",
				Integer.class)).isZero();
	}

	private ResultActions enviar(String url, byte[] planilha) throws Exception {
		return mvc.perform(multipart(url).file(Planilhas.arquivo(planilha)).with(csrf()));
	}

	private static List<Object> linha(Object... valores) {
		return Arrays.asList(valores);
	}

	private void inscrever(String nome) {
		UUID pessoa = UUID.randomUUID();
		jdbc.update("insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)"
				+ " values (?, ?, 'TEA', '3132345678', now(), now())", pessoa, nome);
		jdbc.update("insert into inscricao_lista_espera (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao,"
				+ " criado_em, atualizado_em) values (?, ?, '2010-01-01', 'CRAS', 'AGUARDANDO', now(), now())",
				UUID.randomUUID(), pessoa);
	}

}
