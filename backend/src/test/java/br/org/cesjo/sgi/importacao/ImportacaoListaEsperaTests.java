package br.org.cesjo.sgi.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/** Importação da lista de espera atual de planilha Excel (issue #33, ADR 0215 a ADR 0218). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ImportacaoListaEsperaTests {

	private static final String URL = "/api/importacao/lista-espera";

	private static final List<String> CABECALHO = List.of("Nome completo *", "Data de nascimento", "Tipo de deficiência *",
			"Telefone *", "Nome do responsável *", "Origem do encaminhamento *", "Data de cadastro *");

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	Auditoria auditoria;

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "importacao-modelo")
	void modeloTemCabecalhoEInstrucoes() throws Exception {
		byte[] modelo = mvc.perform(get(URL + "/modelo"))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		try (XSSFWorkbook livro = new XSSFWorkbook(new ByteArrayInputStream(modelo))) {
			DataFormatter formato = new DataFormatter();
			List<String> titulos = new ArrayList<>();
			livro.getSheet("Dados").getRow(0).forEach(celula -> titulos.add(formato.formatCellValue(celula)));
			assertThat(titulos).isEqualTo(CABECALHO);
			assertThat(livro.getSheet("Dados").getLastRowNum()).isZero();
			StringBuilder instrucoes = new StringBuilder();
			livro.getSheet("Instruções").forEach(linha -> linha.forEach(c -> instrucoes.append(formato.formatCellValue(c)).append('|')));
			assertThat(instrucoes).contains("Síndrome de Down (T21)", "Demanda espontânea", "dd/mm/aaaa");
		}
		mvc.perform(get(URL + "/colunas"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].titulo").value("Nome completo"))
			.andExpect(jsonPath("$[1].obrigatoria").value(false));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "importacao-previa")
	void previaMostraErrosPorLinhaSemGravar() throws Exception {
		inscrever("Dora Previafila", "2010-05-05");
		byte[] planilha = Planilhas.xlsx(CABECALHO, List.of(
				linha("Ana Previafila", "15/03/1990", "Intelectual", "(31) 98765-4321", "José", "CRAS", "02/01/2001"),
				linha("Bia Previafila", "31/02/1990", "Visual", "123", "Rosa", "Outra", "02/01/2001"),
				linha("Caio Previafila", null, "TEA", "31987654321", null, "cras", null),
				linha("ana previafila", null, "TEA", "3132345678", "Rui", "CREAS", "03/01/2001"),
				linha("Dora Previafila", null, "TEA", "3132345678", "Rui", "CREAS", "03/01/2001")));

		enviar(URL + "/previa", planilha)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(5))
			.andExpect(jsonPath("$.validas").value(1))
			.andExpect(jsonPath("$.comErro").value(4))
			.andExpect(jsonPath("$.colunas[0]").value("Nome completo"))
			.andExpect(jsonPath("$.linhas[0].linha").value(2))
			.andExpect(jsonPath("$.linhas[0].erros").isEmpty())
			.andExpect(jsonPath("$.linhas[0].valores[6]").value("02/01/2001"))
			.andExpect(jsonPath("$.linhas[1].erros", Matchers.hasItems(
					"Data de nascimento: use uma data válida no formato dd/mm/aaaa.",
					"Telefone: informe DDD e número, com 10 ou 11 dígitos.")))
			.andExpect(jsonPath("$.linhas[1].erros", Matchers.hasItem(Matchers.startsWith("Tipo de deficiência: \"Visual\""))))
			.andExpect(jsonPath("$.linhas[1].erros", Matchers.hasItem(Matchers.startsWith("Origem do encaminhamento: \"Outra\""))))
			.andExpect(jsonPath("$.linhas[2].erros", Matchers.containsInAnyOrder(
					"Preencha a coluna Nome do responsável.", "Preencha a coluna Data de cadastro.")))
			.andExpect(jsonPath("$.linhas[3].erros[0]").value("Nome repetido: já aparece na linha 2 da planilha."))
			.andExpect(jsonPath("$.linhas[4].erros[0]").value("Já há um candidato com este nome na lista de espera."));

		assertThat(contar("Previafila")).isEqualTo(1);
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.IMPORTAR));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "importacao-confirma")
	void confirmacaoGravaSoAsValidasComADataOriginal() throws Exception {
		inscrever("Eva Confirmafila", "1995-06-10");
		byte[] planilha = Planilhas.xlsx(CABECALHO, List.of(
				linha("Gil Confirmafila", null, "Física", "3132345678", "Lia", "Saúde", "20/07/1995"),
				linha("Fabi Confirmafila", LocalDate.of(1980, 1, 2), "T21", 31987654321L, "Leo", "DEMANDA_ESPONTANEA",
						LocalDate.of(1995, 6, 1)),
				linha("Hugo Confirmafila", null, "TEA", "3132345678", "Ivo", "CRAS", "01/01/2999")));

		enviar(URL, planilha)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.importadas").value(2))
			.andExpect(jsonPath("$.ignoradas").value(1));

		List<Map<String, Object>> fila = jdbc.queryForList("select p.nome_completo, p.telefone, p.data_nascimento,"
				+ " i.data_cadastro, i.situacao, i.origem_encaminhamento from inscricao_lista_espera i"
				+ " join pessoa p on p.id = i.pessoa_id where p.nome_completo like '%Confirmafila'"
				+ " order by i.data_cadastro, i.criado_em");
		assertThat(fila).extracting(l -> l.get("nome_completo"))
			.containsExactly("Fabi Confirmafila", "Eva Confirmafila", "Gil Confirmafila");
		assertThat(fila.get(0)).containsEntry("telefone", "31987654321")
			.containsEntry("situacao", "AGUARDANDO")
			.containsEntry("origem_encaminhamento", "DEMANDA_ESPONTANEA");
		assertThat(fila.get(0).get("data_cadastro").toString()).isEqualTo("1995-06-01");
		assertThat(fila.get(0).get("data_nascimento").toString()).isEqualTo("1980-01-02");

		mvc.perform(get("/api/lista-espera/inscricoes").param("nome", "Confirmafila"))
			.andExpect(jsonPath("$.content[*].nomeCompleto").value(Matchers.contains("Fabi Confirmafila",
					"Eva Confirmafila", "Gil Confirmafila")));

		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.IMPORTAR
				&& e.entidade().equals("planilha_lista_espera")));
		verify(auditoria, atLeastOnce()).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR
				&& e.entidade().equals("inscricao_lista_espera")));

		// A mesma planilha de novo não duplica a fila: as linhas já importadas viram erro.
		enviar(URL, planilha).andExpect(status().isUnprocessableEntity());
		assertThat(contar("Confirmafila")).isEqualTo(3);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "importacao-malformada")
	void planilhasMalformadasSaoRecusadas() throws Exception {
		enviar(URL + "/previa", "isto não é planilha".getBytes())
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value("O arquivo não é uma planilha Excel (.xlsx)."));

		enviar(URL + "/previa", new byte[0])
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value("O arquivo enviado está vazio."));

		byte[] truncada = Arrays.copyOf(Planilhas.xlsx(CABECALHO, List.of(linha("X", null, "TEA", "3132345678", "Y",
				"CRAS", "01/01/2001"))), 400);
		enviar(URL + "/previa", truncada)
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value(Matchers.startsWith("Não foi possível ler a planilha.")));

		byte[] semColuna = Planilhas.xlsx(List.of("Nome completo", "Telefone"),
				List.of(List.<Object>of("Zeca", "3132345678")));
		enviar(URL + "/previa", semColuna)
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value(Matchers.containsString("Tipo de deficiência, Nome do responsável,"
					+ " Origem do encaminhamento, Data de cadastro")));

		enviar(URL + "/previa", Planilhas.xlsx(CABECALHO, List.of()))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value("A planilha não tem nenhuma linha de dados abaixo do cabeçalho."));

		byte[] grande = new byte[2 * 1024 * 1024 + 1];
		enviar(URL + "/previa", grande)
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value("A planilha passa do limite de 2 MB. Divida em arquivos menores."));

		List<List<Object>> muitas = new ArrayList<>();
		for (int i = 0; i < 1001; i++) {
			muitas.add(linha("Pessoa " + i, null, "TEA", "3132345678", "R", "CRAS", "01/01/2001"));
		}
		enviar(URL + "/previa", Planilhas.xlsx(CABECALHO, muitas))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value("A planilha passa de 1000 linhas de dados. Divida em arquivos menores."));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "importacao-administrativo")
	void administrativoNaoImporta() throws Exception {
		byte[] planilha = Planilhas.xlsx(CABECALHO, List.of(linha("Ivo Semacesso", null, "TEA", "3132345678", "R",
				"CRAS", "01/01/2001")));
		mvc.perform(get(URL + "/modelo")).andExpect(status().isForbidden());
		enviar(URL + "/previa", planilha).andExpect(status().isForbidden());
		enviar(URL, planilha).andExpect(status().isForbidden());
		assertThat(contar("Semacesso")).isZero();
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "importacao-tecnica")
	void equipeTecnicaNaoImporta() throws Exception {
		mvc.perform(get(URL + "/colunas")).andExpect(status().isForbidden());
	}

	private ResultActions enviar(String url, byte[] planilha) throws Exception {
		return mvc.perform(multipart(url).file(Planilhas.arquivo(planilha)).with(csrf()));
	}

	private static List<Object> linha(Object... valores) {
		return Arrays.asList(valores);
	}

	private void inscrever(String nome, String dataCadastro) {
		UUID pessoa = UUID.randomUUID();
		jdbc.update("insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)"
				+ " values (?, ?, 'TEA', '3132345678', now(), now())", pessoa, nome);
		jdbc.update("insert into inscricao_lista_espera (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao,"
				+ " criado_em, atualizado_em) values (?, ?, ?::date, 'CRAS', 'AGUARDANDO', now(), now())",
				UUID.randomUUID(), pessoa, dataCadastro);
	}

	private int contar(String trecho) {
		return jdbc.queryForObject("select count(*) from inscricao_lista_espera i join pessoa p on p.id = i.pessoa_id"
				+ " where p.nome_completo like ?", Integer.class, "%" + trecho);
	}

}
