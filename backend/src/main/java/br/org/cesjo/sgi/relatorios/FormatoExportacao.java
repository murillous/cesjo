package br.org.cesjo.sgi.relatorios;

import java.util.Locale;

/**
 * Formatos de exportação (ADR 0040). Na URL, o formato vai em minúsculas, por exemplo
 * {@code ?formato=pdf} ou {@code ?formato=xlsx} (ADR 0042).
 */
public enum FormatoExportacao {

	PDF("application/pdf", "pdf"),

	XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

	private final String tipoMime;

	private final String extensao;

	FormatoExportacao(String tipoMime, String extensao) {
		this.tipoMime = tipoMime;
		this.extensao = extensao;
	}

	public String tipoMime() {
		return tipoMime;
	}

	public String extensao() {
		return extensao;
	}

	/**
	 * Converte o parâmetro da URL, sem diferenciar maiúsculas de minúsculas.
	 *
	 * @throws IllegalArgumentException quando o texto não é um formato conhecido
	 */
	public static FormatoExportacao doParametro(String texto) {
		if (texto != null) {
			String normalizado = texto.trim().toLowerCase(Locale.ROOT);
			for (FormatoExportacao formato : values()) {
				if (formato.extensao.equals(normalizado)) {
					return formato;
				}
			}
		}
		throw new IllegalArgumentException("Formato de exportação desconhecido: " + texto);
	}

}
