package br.org.cesjo.sgi.arquivos;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Conteúdos mínimos e fictícios com a assinatura de cada tipo aceito. */
public final class ExemplosDeArquivo {

	private ExemplosDeArquivo() {
	}

	public static byte[] pdf() {
		return "%PDF-1.7\n% documento fictício de teste\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
	}

	public static byte[] jpeg() {
		return new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0,
				(byte) 0xFF, (byte) 0xD9 };
	}

	public static byte[] png() {
		return new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R' };
	}

	/** PDF com exatamente {@code tamanho} bytes. */
	public static byte[] pdfComTamanho(int tamanho) {
		byte[] conteudo = new byte[tamanho];
		Arrays.fill(conteudo, (byte) ' ');
		byte[] cabecalho = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(cabecalho, 0, conteudo, 0, cabecalho.length);
		return conteudo;
	}

}
