package br.org.cesjo.sgi.importacao.interno;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Gera o modelo de planilha para download (ADR 0215): aba "Dados" só com o cabeçalho, e aba "Instruções"
 * com o que preencher em cada coluna, os valores aceitos e um exemplo fictício. O exemplo fica fora da
 * aba "Dados" para não ser importado por engano. As colunas
 * obrigatórias levam " *" no título, que a leitura ignora. As colunas da aba "Dados" ficam com formato
 * texto, para o Excel não trocar dia e mês das datas nem tirar o zero do telefone.
 */
@Component
class GeradorModelo {

	static final String ABA_DADOS = "Dados";

	static final String ABA_INSTRUCOES = "Instruções";

	byte[] gerar(ModeloPlanilha modelo) {
		try (XSSFWorkbook livro = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
			livro.getProperties().getCoreProperties().setTitle("Modelo de importação: " + modelo.titulo());
			livro.getProperties().getCoreProperties().setCreator("CESJO SGI");
			Font negrito = livro.createFont();
			negrito.setBold(true);
			CellStyle cabecalho = livro.createCellStyle();
			cabecalho.setFont(negrito);
			CellStyle texto = livro.createCellStyle();
			texto.setDataFormat(livro.createDataFormat().getFormat("@"));

			List<Coluna> colunas = modelo.colunas();
			XSSFSheet dados = livro.createSheet(ABA_DADOS);
			Row titulos = dados.createRow(0);
			for (int i = 0; i < colunas.size(); i++) {
				Coluna coluna = colunas.get(i);
				var celula = titulos.createCell(i);
				celula.setCellValue(coluna.titulo() + (coluna.obrigatoria() ? " *" : ""));
				celula.setCellStyle(cabecalho);
				dados.setDefaultColumnStyle(i, texto);
				dados.setColumnWidth(i, Math.clamp(coluna.titulo().length() + 6, 14, 40) * 256);
			}
			dados.createFreezePane(0, 1);

			XSSFSheet instrucoes = livro.createSheet(ABA_INSTRUCOES);
			Row topo = instrucoes.createRow(0);
			String[] rotulos = { "Coluna", "Obrigatória", "O que preencher", "Valores aceitos", "Exemplo" };
			for (int i = 0; i < rotulos.length; i++) {
				var celula = topo.createCell(i);
				celula.setCellValue(rotulos[i]);
				celula.setCellStyle(cabecalho);
			}
			for (int i = 0; i < colunas.size(); i++) {
				Coluna coluna = colunas.get(i);
				Row linha = instrucoes.createRow(i + 1);
				linha.createCell(0).setCellValue(coluna.titulo());
				linha.createCell(1).setCellValue(coluna.obrigatoria() ? "Sim" : "Não");
				linha.createCell(2).setCellValue(coluna.formato());
				linha.createCell(3).setCellValue(String.join("; ", coluna.valoresAceitos()));
				linha.createCell(4).setCellValue(coluna.exemplo());
			}
			Row nota = instrucoes.createRow(colunas.size() + 2);
			nota.createCell(0)
				.setCellValue("Preencha a aba Dados, uma pessoa por linha, a partir da linha 2, sem mudar os títulos"
						+ " da linha 1. Datas no formato dd/mm/aaaa. A ordem das colunas não importa.");
			int[] larguras = { 28, 12, 60, 70, 30 };
			for (int i = 0; i < larguras.length; i++) {
				instrucoes.setColumnWidth(i, larguras[i] * 256);
			}

			livro.write(saida);
			return saida.toByteArray();
		}
		catch (IOException erro) {
			throw new UncheckedIOException("Falha ao gerar o modelo de planilha", erro);
		}
	}

}
