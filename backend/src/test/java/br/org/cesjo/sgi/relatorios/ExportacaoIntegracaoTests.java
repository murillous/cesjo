package br.org.cesjo.sgi.relatorios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.Auditoria;

/**
 * Fluxo principal da exportação (issue #55): um controlador de mentira, como o de um módulo de negócio,
 * gera PDF e XLSX de verdade pela {@link Exportacao}; o teste abre os arquivos, confere o conteúdo, os
 * cabeçalhos do download e o registro na auditoria. Só dados fictícios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, ExportacaoIntegracaoTests.ControladorDeTeste.class })
class ExportacaoIntegracaoTests {

	private static final String URL = "/api/teste-exportacao";

	private static final String LOGIN = "exportacao-coordenador";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	Exportacao exportacao;

	@MockitoSpyBean
	Auditoria auditoria;

	@AfterEach
	void restaurarAuditoria() {
		reset(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = LOGIN)
	void exportaPdfComTituloFiltrosColunasELinhas() throws Exception {
		String entidade = entidadeUnica();

		MvcResult resposta = mvc.perform(get(URL).param("formato", "pdf").param("entidade", entidade))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/pdf"))
			.andExpect(header().string("Content-Disposition",
					org.hamcrest.Matchers.matchesPattern("attachment; filename=\"candidatos-ficticios-\\d{4}-\\d{2}-\\d{2}\\.pdf\"; filename\\*=UTF-8''.+")))
			.andExpect(header().string("Cache-Control", "no-store"))
			.andReturn();

		byte[] pdf = resposta.getResponse().getContentAsByteArray();
		assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
		try (PDDocument documento = Loader.loadPDF(pdf)) {
			String texto = new PDFTextStripper().getText(documento);
			assertThat(texto).contains("Candidatos fictícios", "Gerado em", "Situação: Aguardando",
					"Origem: CRAS", "Nome", "Data de cadastro", "Pessoa Fictícia Um", "=1+1", "03/11/2026 14:30",
					"02/11/2026", "Sim", "Não", "1234,5", "2 registros", "Página 1 de 1");
			assertThat(documento.getDocumentInformation().getTitle()).isEqualTo("Candidatos fictícios");
			PDRectangle pagina = documento.getPage(0).getMediaBox();
			assertThat(pagina.getHeight()).as("retrato com poucas colunas").isGreaterThan(pagina.getWidth());
		}
		assertThat(exportacoesRegistradas(entidade)).isEqualTo(1);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = LOGIN)
	void exportaPlanilhaComValoresTipados() throws Exception {
		String entidade = entidadeUnica();

		MvcResult resposta = mvc.perform(get(URL).param("formato", "XLSX").param("entidade", entidade))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", FormatoExportacao.XLSX.tipoMime()))
			.andExpect(header().string("Content-Disposition",
					org.hamcrest.Matchers.containsString(".xlsx\";")))
			.andReturn();

		try (XSSFWorkbook planilha = new XSSFWorkbook(
				new ByteArrayInputStream(resposta.getResponse().getContentAsByteArray()))) {
			XSSFSheet aba = planilha.getSheetAt(0);
			assertThat(aba.getSheetName()).isEqualTo("Candidatos fictícios");
			assertThat(aba.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Candidatos fictícios");
			assertThat(aba.getRow(1).getCell(0).getStringCellValue()).startsWith("Gerado em ");
			assertThat(aba.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Situação");
			assertThat(aba.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Aguardando");
			assertThat(aba.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Origem");

			int cabecalho = 5;
			assertThat(aba.getRow(cabecalho).getCell(0).getStringCellValue()).isEqualTo("Nome");
			assertThat(aba.getRow(cabecalho).getCell(4).getStringCellValue()).isEqualTo("Valor");
			assertThat(aba.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) (cabecalho + 1));

			var primeira = aba.getRow(cabecalho + 1);
			assertThat(primeira.getCell(0).getStringCellValue()).isEqualTo("Pessoa Fictícia Um");
			assertThat(primeira.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
			assertThat(DateUtil.isCellDateFormatted(primeira.getCell(1))).isTrue();
			assertThat(primeira.getCell(1).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 2));
			assertThat(primeira.getCell(2).getStringCellValue()).isEqualTo("Sim");
			assertThat(primeira.getCell(3).getNumericCellValue()).isEqualTo(3);
			assertThat(primeira.getCell(4).getNumericCellValue()).isEqualTo(1234.5);

			var segunda = aba.getRow(cabecalho + 2);
			assertThat(segunda.getCell(0).getStringCellValue()).as("texto nunca vira fórmula").isEqualTo("=1+1");
			assertThat(segunda.getCell(0).getCellType()).isEqualTo(CellType.STRING);
			assertThat(segunda.getCell(4).getCellType()).isEqualTo(CellType.BLANK);
			assertThat(aba.getLastRowNum()).isEqualTo(cabecalho + 2);
		}
		assertThat(exportacoesRegistradas(entidade)).isEqualTo(1);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = LOGIN)
	void tabelaVaziaComMuitasColunasSaiEmPaisagemComAviso() {
		List<String> colunas = new ArrayList<>();
		for (int i = 1; i <= 8; i++) {
			colunas.add("Coluna " + i);
		}

		ArquivoExportado arquivo = exportacao.exportar(entidadeUnica(),
				new TabelaExportacao("Relatório vazio", colunas, List.of()), FormatoExportacao.PDF);

		try (PDDocument documento = Loader.loadPDF(arquivo.conteudo())) {
			assertThat(new PDFTextStripper().getText(documento)).contains("Nenhum registro encontrado.",
					"Sem filtros: todos os registros.", "Coluna 8", "0 registros");
			PDRectangle pagina = documento.getPage(0).getMediaBox();
			assertThat(pagina.getWidth()).isGreaterThan(pagina.getHeight());
		}
		catch (java.io.IOException erro) {
			throw new java.io.UncheckedIOException(erro);
		}
		assertThat(arquivo.nomeArquivo()).matches("relatorio-vazio-\\d{4}-\\d{2}-\\d{2}\\.pdf");
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = LOGIN)
	void falhaNaAuditoriaImpedeOArquivoDeSair() {
		String entidade = entidadeUnica();
		doThrow(new IllegalStateException("auditoria fora do ar")).when(auditoria).registrar(any());

		assertThatThrownBy(() -> exportacao.exportar(entidade, tabela(), FormatoExportacao.XLSX))
			.isInstanceOf(IllegalStateException.class);
		assertThat(exportacoesRegistradas(entidade)).isZero();
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = LOGIN)
	void formatoDesconhecidoEhRecusadoSemRegistrar() throws Exception {
		String entidade = entidadeUnica();

		mvc.perform(get(URL).param("formato", "csv").param("entidade", entidade)).andExpect(status().isBadRequest());

		assertThat(exportacoesRegistradas(entidade)).isZero();
	}

	@Test
	void semLoginNaoExporta() throws Exception {
		String entidade = entidadeUnica();

		mvc.perform(get(URL).param("formato", "pdf").param("entidade", entidade)).andExpect(status().isUnauthorized());

		assertThat(exportacoesRegistradas(entidade)).isZero();
	}

	private long exportacoesRegistradas(String entidade) {
		return jdbc
			.sql("select count(*) from auditoria where entidade = ? and acao = 'EXPORTAR' and conta_login = ?")
			.param(entidade)
			.param(LOGIN)
			.query(Long.class)
			.single();
	}

	private static String entidadeUnica() {
		return "teste_exportacao_" + UUID.randomUUID().toString().replace("-", "");
	}

	static TabelaExportacao tabela() {
		List<Object> um = new ArrayList<>(
				List.of("Pessoa Fictícia Um", LocalDate.of(2026, 11, 2), true, 3, new BigDecimal("1234.5")));
		List<Object> dois = new ArrayList<>(List.of("=1+1", LocalDateTime.of(2026, 11, 3, 14, 30), false, 0L));
		dois.add(null);
		return new TabelaExportacao("Candidatos fictícios",
				List.of("Nome", "Data de cadastro", "BPC", "Documentos", "Valor"), List.of(um, dois),
				List.of(new FiltroAplicado("Situação", "Aguardando"), new FiltroAplicado("Origem", "CRAS")));
	}

	/** Controlador de mentira, como o de um módulo que exporta os seus dados. */
	@RestController
	static class ControladorDeTeste {

		private final Exportacao exportacao;

		ControladorDeTeste(Exportacao exportacao) {
			this.exportacao = exportacao;
		}

		@GetMapping(URL)
		ResponseEntity<byte[]> exportar(@RequestParam FormatoExportacao formato, @RequestParam String entidade) {
			return exportacao.exportar(entidade, tabela(), formato).comoDownload();
		}

	}

}
