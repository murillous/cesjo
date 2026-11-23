package br.org.cesjo.sgi.importacao.interno;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Lê a primeira aba de uma planilha {@code .xlsx} enviada para importação (ADR 0215). O cabeçalho é a
 * primeira linha com algum valor; as colunas são achadas pelo título, em qualquer ordem, e as colunas
 * desconhecidas são ignoradas. Linhas totalmente vazias são puladas.
 *
 * <p>
 * Os limites de tamanho e de linhas protegem a memória do servidor: a planilha é lida inteira (ADR 0218).
 * Arquivo vazio, grande demais, sem cabeçalho, sem coluna obrigatória, com linhas demais, protegido por
 * senha ou que não é {@code .xlsx} resulta em {@link RegraNegocioException} (422), com a mensagem para
 * quem enviou.
 */
@Component
class LeitorPlanilha {

	/** Tamanho máximo do arquivo, 2 MB: sobra para alguns milhares de linhas de cadastro. */
	static final long TAMANHO_MAXIMO = 2L * 1024 * 1024;

	/** Linhas de dados por planilha, sem contar o cabeçalho. */
	static final int LINHAS_MAXIMAS = 1000;

	List<LinhaPlanilha> ler(byte[] conteudo, List<Coluna> colunas) {
		if (conteudo == null || conteudo.length == 0) {
			throw new RegraNegocioException("O arquivo enviado está vazio.");
		}
		if (conteudo.length > TAMANHO_MAXIMO) {
			throw new RegraNegocioException("A planilha passa do limite de 2 MB. Divida em arquivos menores.");
		}
		exigirXlsx(conteudo);
		try (XSSFWorkbook livro = new XSSFWorkbook(new ByteArrayInputStream(conteudo))) {
			if (livro.getNumberOfSheets() == 0) {
				throw new RegraNegocioException("A planilha não tem nenhuma aba.");
			}
			return linhas(livro.getSheetAt(0), colunas);
		}
		catch (RegraNegocioException erro) {
			throw erro;
		}
		catch (EncryptedDocumentException erro) {
			throw new RegraNegocioException("A planilha está protegida por senha. Tire a senha e envie de novo.");
		}
		catch (IOException | RuntimeException erro) {
			// Arquivo .xlsx corrompido, incompleto ou com estrutura que o POI recusa (inclusive zip bomb).
			throw new RegraNegocioException(
					"Não foi possível ler a planilha. Confira se o arquivo é um .xlsx válido, salvo pelo Excel ou LibreOffice.");
		}
	}

	private static void exigirXlsx(byte[] conteudo) {
		FileMagic tipo;
		try (InputStream entrada = new BufferedInputStream(new ByteArrayInputStream(conteudo))) {
			tipo = FileMagic.valueOf(FileMagic.prepareToCheckMagic(entrada));
		}
		catch (IOException erro) {
			tipo = FileMagic.UNKNOWN;
		}
		switch (tipo) {
			case OOXML -> {
			}
			case OLE2 -> throw new RegraNegocioException(
					"A planilha está no formato antigo (.xls) ou protegida por senha. Salve como .xlsx, sem senha, e envie de novo.");
			default -> throw new RegraNegocioException("O arquivo não é uma planilha Excel (.xlsx).");
		}
	}

	private static List<LinhaPlanilha> linhas(Sheet aba, List<Coluna> colunas) {
		Row cabecalho = null;
		for (Row linha : aba) {
			if (!vazia(linha)) {
				cabecalho = linha;
				break;
			}
		}
		if (cabecalho == null) {
			throw new RegraNegocioException("A planilha está vazia: falta a linha de cabeçalho com os títulos das colunas.");
		}
		Map<Coluna, Integer> posicoes = posicoes(cabecalho, colunas);

		List<LinhaPlanilha> linhas = new ArrayList<>();
		for (int numero = cabecalho.getRowNum() + 1; numero <= aba.getLastRowNum(); numero++) {
			Row linha = aba.getRow(numero);
			if (linha == null || vazia(linha)) {
				continue;
			}
			if (linhas.size() == LINHAS_MAXIMAS) {
				throw new RegraNegocioException(
						"A planilha passa de " + LINHAS_MAXIMAS + " linhas de dados. Divida em arquivos menores.");
			}
			Map<Coluna, Object> valores = new HashMap<>();
			posicoes.forEach((coluna, posicao) -> {
				Object valor = valor(linha.getCell(posicao));
				if (valor != null) {
					valores.put(coluna, valor);
				}
			});
			linhas.add(new LinhaPlanilha(numero + 1, valores));
		}
		if (linhas.isEmpty()) {
			throw new RegraNegocioException("A planilha não tem nenhuma linha de dados abaixo do cabeçalho.");
		}
		return linhas;
	}

	private static Map<Coluna, Integer> posicoes(Row cabecalho, List<Coluna> colunas) {
		Map<String, Integer> porTitulo = new HashMap<>();
		for (Cell celula : cabecalho) {
			Object valor = valor(celula);
			if (valor != null) {
				porTitulo.putIfAbsent(Texto.normalizar(valor.toString()), celula.getColumnIndex());
			}
		}
		Map<Coluna, Integer> posicoes = new HashMap<>();
		List<String> faltando = new ArrayList<>();
		for (Coluna coluna : colunas) {
			Integer posicao = porTitulo.get(Texto.normalizar(coluna.titulo()));
			if (posicao != null) {
				posicoes.put(coluna, posicao);
			}
			else if (coluna.obrigatoria()) {
				faltando.add(coluna.titulo());
			}
		}
		if (!faltando.isEmpty()) {
			throw new RegraNegocioException("A planilha não tem " + (faltando.size() == 1 ? "a coluna obrigatória " : "as colunas obrigatórias ")
					+ String.join(", ", faltando) + ". Use o modelo de planilha.");
		}
		return posicoes;
	}

	private static boolean vazia(Row linha) {
		for (Cell celula : linha) {
			if (valor(celula) != null) {
				return false;
			}
		}
		return true;
	}

	/** {@code String} sem espaços nas pontas, {@code LocalDate} para célula de data ou {@code null} se vazia. */
	private static Object valor(Cell celula) {
		if (celula == null) {
			return null;
		}
		CellType tipo = celula.getCellType() == CellType.FORMULA ? celula.getCachedFormulaResultType()
				: celula.getCellType();
		Object valor = switch (tipo) {
			case STRING -> celula.getStringCellValue().strip();
			case NUMERIC -> DateUtil.isCellDateFormatted(celula) ? celula.getLocalDateTimeCellValue().toLocalDate()
					: BigDecimal.valueOf(celula.getNumericCellValue()).stripTrailingZeros().toPlainString();
			case BOOLEAN -> celula.getBooleanCellValue() ? "VERDADEIRO" : "FALSO";
			case ERROR -> "#ERRO";
			default -> null;
		};
		return "".equals(valor) ? null : valor;
	}

}
