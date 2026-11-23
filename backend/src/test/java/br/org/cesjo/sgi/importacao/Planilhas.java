package br.org.cesjo.sgi.importacao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;

/** Monta planilhas .xlsx de teste, com dados fictícios. */
final class Planilhas {

	private Planilhas() {
	}

	/**
	 * Planilha com uma aba: a primeira linha é o cabeçalho e as demais os dados. Valor {@code LocalDate} vira
	 * célula de data, {@code Number} vira célula numérica e {@code null} fica vazio.
	 */
	static byte[] xlsx(List<String> cabecalho, List<List<Object>> linhas) {
		try (XSSFWorkbook livro = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
			CellStyle data = livro.createCellStyle();
			data.setDataFormat(livro.createDataFormat().getFormat("dd/mm/yyyy"));
			XSSFSheet aba = livro.createSheet("Dados");
			Row topo = aba.createRow(0);
			for (int i = 0; i < cabecalho.size(); i++) {
				topo.createCell(i).setCellValue(cabecalho.get(i));
			}
			for (int l = 0; l < linhas.size(); l++) {
				Row linha = aba.createRow(l + 1);
				List<Object> valores = linhas.get(l);
				for (int i = 0; i < valores.size(); i++) {
					Object valor = valores.get(i);
					if (valor == null) {
						continue;
					}
					Cell celula = linha.createCell(i);
					switch (valor) {
						case LocalDate d -> {
							celula.setCellValue(d);
							celula.setCellStyle(data);
						}
						case Number n -> celula.setCellValue(n.doubleValue());
						default -> celula.setCellValue(valor.toString());
					}
				}
			}
			livro.write(saida);
			return saida.toByteArray();
		}
		catch (IOException erro) {
			throw new UncheckedIOException(erro);
		}
	}

	static MockMultipartFile arquivo(byte[] conteudo) {
		return new MockMultipartFile("arquivo", "planilha.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", conteudo);
	}

}
