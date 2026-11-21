package br.org.cesjo.sgi.listaespera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

/** Exportação da fila em PDF e Excel com os filtros da tela (issue #32, ADR 0130). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ExportacaoListaEsperaTests {

	private static final String URL = "/api/lista-espera/exportacao";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	Auditoria auditoria;

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "exportacao-planilha")
	void planilhaTemAFilaFiltradaNaOrdemSemContato() throws Exception {
		inserir("Bia Exportafila", "TEA", "CRAS", "2015-01-02");
		inserir("Caio Exportafila", "FISICA", "SAUDE", "2015-01-03");
		inserir("Ana Exportafila", "TEA", "CREAS", "2015-01-01");

		byte[] planilha = mvc.perform(get(URL).param("formato", "xlsx").param("nome", "exportafila")
			.param("tipoDeficiencia", "TEA"))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		List<String> texto = new ArrayList<>();
		try (XSSFWorkbook livro = new XSSFWorkbook(new ByteArrayInputStream(planilha))) {
			Sheet aba = livro.getSheetAt(0);
			DataFormatter formatador = new DataFormatter();
			for (Row linha : aba) {
				StringBuilder conteudo = new StringBuilder();
				linha.forEach(celula -> conteudo.append(formatador.formatCellValue(celula)).append('|'));
				texto.add(conteudo.toString());
			}
		}
		String tudo = String.join("\n", texto);
		assertThat(tudo).contains("Lista de espera", "Nome contém", "exportafila", "Tipo de deficiência");
		assertThat(tudo.indexOf("Ana Exportafila")).isPositive().isLessThan(tudo.indexOf("Bia Exportafila"));
		assertThat(tudo).doesNotContain("Caio Exportafila", "3132345678");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.EXPORTAR && e.entidade().equals("lista_espera")));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "exportacao-pdf")
	void pdfSaiComoDownload() throws Exception {
		mvc.perform(get(URL).param("formato", "pdf").param("situacao", "AGUARDANDO"))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/pdf"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "exportacao-formato")
	void formatoInvalidoResponde400SemAuditar() throws Exception {
		mvc.perform(get(URL).param("formato", "csv")).andExpect(status().isBadRequest());
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.EXPORTAR));
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "exportacao-consulta")
	void consultaNaoExporta() throws Exception {
		mvc.perform(get(URL).param("formato", "pdf")).andExpect(status().isForbidden());
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.EXPORTAR));
	}

	private void inserir(String nome, String tipo, String origem, String data) {
		UUID pessoa = UUID.randomUUID();
		jdbc.update("insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)"
				+ " values (?, ?, ?, '3132345678', now(), now())", pessoa, nome, tipo);
		jdbc.update("insert into inscricao_lista_espera (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao,"
				+ " criado_em, atualizado_em) values (?, ?, ?::date, ?, 'AGUARDANDO', now(), now())", UUID.randomUUID(),
				pessoa, data, origem);
	}

}
