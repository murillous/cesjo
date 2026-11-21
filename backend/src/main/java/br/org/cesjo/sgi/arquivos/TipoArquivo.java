package br.org.cesjo.sgi.arquivos;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Tipos de arquivo aceitos (ADR 0027). O tipo é reconhecido pela assinatura dos primeiros bytes do
 * conteúdo, nunca pela extensão do nome nem pelo {@code Content-Type} informado pelo cliente.
 */
public enum TipoArquivo {

	PDF("application/pdf", List.of(".pdf"), new byte[] { '%', 'P', 'D', 'F', '-' }),

	JPEG("image/jpeg", List.of(".jpg", ".jpeg"), new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }),

	PNG("image/png", List.of(".png"),
			new byte[] { (byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A });

	private final String tipoMime;

	private final List<String> extensoes;

	private final byte[] assinatura;

	TipoArquivo(String tipoMime, List<String> extensoes, byte[] assinatura) {
		this.tipoMime = tipoMime;
		this.extensoes = extensoes;
		this.assinatura = assinatura;
	}

	public String tipoMime() {
		return tipoMime;
	}

	/** Extensões aceitas no nome, em minúsculas; a primeira é a usada quando o nome não tem nenhuma. */
	public List<String> extensoes() {
		return extensoes;
	}

	/** Reconhece o tipo pelos primeiros bytes do conteúdo. Vazio quando o tipo não é aceito. */
	public static Optional<TipoArquivo> detectar(byte[] conteudo) {
		if (conteudo == null) {
			return Optional.empty();
		}
		return Arrays.stream(values()).filter(tipo -> tipo.confere(conteudo)).findFirst();
	}

	public static TipoArquivo doTipoMime(String tipoMime) {
		return Arrays.stream(values())
			.filter(tipo -> tipo.tipoMime.equals(tipoMime))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Tipo MIME desconhecido: " + tipoMime));
	}

	/** Diz se o nome termina numa extensão deste tipo, sem diferenciar maiúsculas. */
	public boolean temExtensao(String nome) {
		String minusculo = nome.toLowerCase(Locale.ROOT);
		return extensoes.stream().anyMatch(minusculo::endsWith);
	}

	private boolean confere(byte[] conteudo) {
		if (conteudo.length < assinatura.length) {
			return false;
		}
		for (int i = 0; i < assinatura.length; i++) {
			if (conteudo[i] != assinatura[i]) {
				return false;
			}
		}
		return true;
	}

}
