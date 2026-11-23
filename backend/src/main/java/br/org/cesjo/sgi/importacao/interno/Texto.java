package br.org.cesjo.sgi.importacao.interno;

import java.text.Normalizer;
import java.util.Locale;

/** Comparação tolerante de textos da planilha: sem acentos, maiúsculas, sublinhados nem espaços extras. */
final class Texto {

	private Texto() {
	}

	static String normalizar(String texto) {
		if (texto == null) {
			return "";
		}
		String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return semAcento.toLowerCase(Locale.ROOT).replaceAll("[_*]", " ").replaceAll("\\s+", " ").strip();
	}

}
