package br.org.cesjo.sgi.relatorios.interno;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.relatorios.FiltroAplicado;
import br.org.cesjo.sgi.relatorios.TabelaExportacao;

/**
 * Gera a planilha Excel (XLSX) da tabela com o Apache POI (ADR 0040).
 *
 * <p>
 * Layout: título, data de geração e filtros nas primeiras linhas, uma linha em branco, o cabeçalho com
 * as cores institucionais, congelado e com filtro automático, e os dados. Números e datas são gravados
 * como valores, não como texto. A largura das colunas é estimada pelo número de caracteres:
 * {@code autoSizeColumn} depende das fontes do sistema, que a imagem Docker pode não ter.
 */
@Component
class GeradorPlanilha {

	/** Largura máxima de uma coluna, em caracteres. */
	static final int LARGURA_MAXIMA = 60;

	private static final int LARGURA_MINIMA = 8;

	byte[] gerar(TabelaExportacao tabela, Instant geradoEm) {
		try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
			Estilos estilos = new Estilos(planilha);
			XSSFSheet aba = planilha.createSheet(WorkbookUtil.createSafeSheetName(tabela.titulo()));
			planilha.getProperties().getCoreProperties().setTitle(tabela.titulo());
			planilha.getProperties().getCoreProperties().setCreator("CESJO SGI");

			int numeroLinha = 0;
			texto(aba.createRow(numeroLinha++), 0, tabela.titulo(), estilos.titulo);
			texto(aba.createRow(numeroLinha++), 0, "Gerado em " + FormatacaoCelula.dataHora(geradoEm), estilos.nota);
			if (tabela.filtros().isEmpty()) {
				texto(aba.createRow(numeroLinha++), 0, "Sem filtros: todos os registros.", estilos.nota);
			}
			for (FiltroAplicado filtro : tabela.filtros()) {
				Row linha = aba.createRow(numeroLinha++);
				texto(linha, 0, filtro.nome(), estilos.nomeFiltro);
				texto(linha, 1, filtro.valor(), estilos.nota);
			}
			numeroLinha++;

			int linhaCabecalho = numeroLinha;
			int[] larguras = new int[tabela.colunas().size()];
			Row cabecalho = aba.createRow(numeroLinha++);
			for (int coluna = 0; coluna < tabela.colunas().size(); coluna++) {
				String rotulo = tabela.colunas().get(coluna);
				texto(cabecalho, coluna, rotulo, estilos.cabecalho);
				larguras[coluna] = rotulo.length();
			}

			for (List<Object> valores : tabela.linhas()) {
				Row linha = aba.createRow(numeroLinha++);
				for (int coluna = 0; coluna < valores.size(); coluna++) {
					Object valor = valores.get(coluna);
					celula(linha.createCell(coluna), valor, estilos);
					larguras[coluna] = Math.max(larguras[coluna], FormatacaoCelula.texto(valor).length());
				}
			}

			for (int coluna = 0; coluna < larguras.length; coluna++) {
				int caracteres = Math.clamp(larguras[coluna] + 2, LARGURA_MINIMA, LARGURA_MAXIMA);
				aba.setColumnWidth(coluna, caracteres * 256);
			}
			aba.createFreezePane(0, linhaCabecalho + 1);
			aba.setAutoFilter(new CellRangeAddress(linhaCabecalho, Math.max(linhaCabecalho, numeroLinha - 1), 0,
					tabela.colunas().size() - 1));

			planilha.write(saida);
			return saida.toByteArray();
		}
		catch (IOException erro) {
			throw new UncheckedIOException("Falha ao gerar a planilha da exportação", erro);
		}
	}

	private static void celula(Cell celula, Object valor, Estilos estilos) {
		switch (valor) {
			case null -> celula.setBlank();
			case Number numero -> {
				celula.setCellValue(numero instanceof BigDecimal decimal ? decimal.doubleValue() : numero.doubleValue());
				celula.setCellStyle(estilos.dado);
			}
			case LocalDate data -> {
				celula.setCellValue(data);
				celula.setCellStyle(estilos.data);
			}
			case LocalDateTime dataHora -> {
				celula.setCellValue(dataHora);
				celula.setCellStyle(estilos.dataHora);
			}
			case Instant instante -> {
				celula.setCellValue(LocalDateTime.ofInstant(instante, IdentidadeVisual.FUSO));
				celula.setCellStyle(estilos.dataHora);
			}
			// Texto é sempre gravado como texto: um valor que começa com "=" não vira fórmula.
			default -> {
				celula.setCellValue(FormatacaoCelula.texto(valor));
				celula.setCellStyle(estilos.dado);
			}
		}
	}

	private static void texto(Row linha, int coluna, String valor, CellStyle estilo) {
		Cell celula = linha.createCell(coluna);
		celula.setCellValue(valor);
		celula.setCellStyle(estilo);
	}

	/** Estilos criados uma vez por planilha: o XLSX tem limite de estilos. */
	private static final class Estilos {

		final CellStyle titulo;

		final CellStyle nota;

		final CellStyle nomeFiltro;

		final CellStyle cabecalho;

		final CellStyle dado;

		final CellStyle data;

		final CellStyle dataHora;

		Estilos(XSSFWorkbook planilha) {
			XSSFColor primariaEscura = cor(planilha, IdentidadeVisual.PRIMARIA_ESCURA);
			XSSFColor borda = cor(planilha, IdentidadeVisual.BORDA);

			XSSFFont fonteTitulo = planilha.createFont();
			fonteTitulo.setBold(true);
			fonteTitulo.setFontHeightInPoints((short) 14);
			fonteTitulo.setColor(primariaEscura);
			titulo = planilha.createCellStyle();
			titulo.setFont(fonteTitulo);

			XSSFFont fonteNota = planilha.createFont();
			fonteNota.setColor(cor(planilha, IdentidadeVisual.TEXTO_SECUNDARIO));
			nota = planilha.createCellStyle();
			nota.setFont(fonteNota);

			XSSFFont fonteNomeFiltro = planilha.createFont();
			fonteNomeFiltro.setBold(true);
			fonteNomeFiltro.setColor(primariaEscura);
			nomeFiltro = planilha.createCellStyle();
			nomeFiltro.setFont(fonteNomeFiltro);

			XSSFFont fonteCabecalho = planilha.createFont();
			fonteCabecalho.setBold(true);
			fonteCabecalho.setColor(cor(planilha, "#FFFFFF"));
			XSSFCellStyle estiloCabecalho = planilha.createCellStyle();
			estiloCabecalho.setFont(fonteCabecalho);
			estiloCabecalho.setFillForegroundColor(primariaEscura);
			estiloCabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			cabecalho = estiloCabecalho;

			dado = comBorda(planilha, borda);
			data = comBorda(planilha, borda);
			data.setDataFormat(planilha.createDataFormat().getFormat("dd/mm/yyyy"));
			dataHora = comBorda(planilha, borda);
			dataHora.setDataFormat(planilha.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
		}

		private static XSSFCellStyle comBorda(XSSFWorkbook planilha, XSSFColor cor) {
			XSSFCellStyle estilo = planilha.createCellStyle();
			estilo.setBorderBottom(BorderStyle.THIN);
			estilo.setBottomBorderColor(cor);
			estilo.setVerticalAlignment(VerticalAlignment.TOP);
			return estilo;
		}

		private static XSSFColor cor(XSSFWorkbook planilha, String hex) {
			byte[] rgb = HexFormat.of().parseHex(hex.substring(1));
			return new XSSFColor(rgb, planilha.getStylesSource().getIndexedColors());
		}

	}

}
