package br.org.cesjo.sgi.compartilhado.criptografia;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * Cifra e decifra bytes e textos com AES-256-GCM (ADR 0004). O formato do valor cifrado está no
 * ADR 0025.
 *
 * <p>
 * Rotação: todo valor carrega o identificador da chave que o cifrou. A decifração usa essa chave, e a
 * cifração usa sempre a chave ativa. Para trocar de chave, acrescenta-se a nova em
 * {@code sgi.criptografia.chaves}, aponta-se {@code sgi.criptografia.chave-ativa} para ela e mantém-se a
 * antiga enquanto houver valor cifrado com ela.
 *
 * <p>
 * O construtor valida a configuração. Sem chave ativa válida a aplicação não sobe.
 */
@Component
public class Cifrador {

	/** Versão do formato do valor cifrado (ADR 0025). */
	static final byte VERSAO = 1;

	static final String PREFIXO_TEXTO = "v1:";

	static final int TAMANHO_CHAVE = 32;

	static final int TAMANHO_IV = 12;

	static final int TAMANHO_ETIQUETA_BITS = 128;

	static final Pattern FORMATO_ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

	private static final String TRANSFORMACAO = "AES/GCM/NoPadding";

	private final SecureRandom aleatorio = new SecureRandom();

	private final String chaveAtiva;

	private final Map<String, SecretKey> chaves;

	public Cifrador(PropriedadesCriptografia propriedades) {
		this.chaves = carregarChaves(propriedades);
		String ativa = propriedades.chaveAtiva();
		if (ativa == null || ativa.isBlank()) {
			throw new ChaveCriptografiaInvalidaException(
					"Chave de criptografia ativa não configurada. Defina sgi.criptografia.chave-ativa "
							+ "(variável SGI_CRIPTOGRAFIA_CHAVEATIVA) e a chave correspondente em "
							+ "sgi.criptografia.chaves.<id> (ADR 0004).");
		}
		if (!chaves.containsKey(ativa)) {
			throw new ChaveCriptografiaInvalidaException("A chave ativa '" + ativa
					+ "' não está em sgi.criptografia.chaves. Defina sgi.criptografia.chaves." + ativa
					+ " (variável SGI_CRIPTOGRAFIA_CHAVES_" + ativa.toUpperCase(Locale.ROOT) + ").");
		}
		this.chaveAtiva = ativa;
	}

	private static Map<String, SecretKey> carregarChaves(PropriedadesCriptografia propriedades) {
		Map<String, SecretKey> carregadas = new HashMap<>();
		propriedades.chaves().forEach((id, base64) -> {
			if (!FORMATO_ID.matcher(id).matches()) {
				throw new ChaveCriptografiaInvalidaException("Identificador de chave inválido: '" + id
						+ "'. Use de 1 a 32 caracteres entre letras minúsculas, dígitos e hífen.");
			}
			byte[] bytes;
			try {
				bytes = Base64.getDecoder().decode(base64 == null ? "" : base64.trim());
			}
			catch (IllegalArgumentException erro) {
				throw new ChaveCriptografiaInvalidaException(
						"A chave '" + id + "' não está em Base64 válido.");
			}
			if (bytes.length != TAMANHO_CHAVE) {
				throw new ChaveCriptografiaInvalidaException("A chave '" + id + "' tem " + bytes.length
						+ " bytes; o AES-256 exige " + TAMANHO_CHAVE + ".");
			}
			carregadas.put(id, new SecretKeySpec(bytes, "AES"));
		});
		return Map.copyOf(carregadas);
	}

	/** Identificador da chave usada para cifrar valores novos. */
	public String chaveAtiva() {
		return chaveAtiva;
	}

	/** Cifra bytes com a chave ativa no formato binário do ADR 0025. */
	public byte[] cifrar(byte[] claro) {
		byte[] cabecalho = cabecalhoBinario(chaveAtiva);
		byte[] iv = novoIv();
		byte[] cifrado = executar(Cipher.ENCRYPT_MODE, chaves.get(chaveAtiva), iv, cabecalho, claro);
		return ByteBuffer.allocate(cabecalho.length + iv.length + cifrado.length)
			.put(cabecalho)
			.put(iv)
			.put(cifrado)
			.array();
	}

	/** Decifra bytes no formato binário do ADR 0025, com a chave indicada no próprio valor. */
	public byte[] decifrar(byte[] valor) {
		String id = chaveDoValor(valor);
		int inicioIv = 2 + id.length();
		if (valor.length < inicioIv + TAMANHO_IV + TAMANHO_ETIQUETA_BITS / 8) {
			throw new ValorCifradoInvalidoException("Valor cifrado truncado.");
		}
		byte[] cabecalho = Arrays.copyOfRange(valor, 0, inicioIv);
		byte[] iv = Arrays.copyOfRange(valor, inicioIv, inicioIv + TAMANHO_IV);
		byte[] cifrado = Arrays.copyOfRange(valor, inicioIv + TAMANHO_IV, valor.length);
		return executar(Cipher.DECRYPT_MODE, chave(id), iv, cabecalho, cifrado);
	}

	/** Identificador da chave que cifrou um valor binário. */
	public String chaveDoValor(byte[] valor) {
		if (valor == null || valor.length < 3 || valor[0] != VERSAO) {
			throw new ValorCifradoInvalidoException("Formato de valor cifrado desconhecido.");
		}
		int tamanhoId = valor[1];
		if (tamanhoId < 1 || tamanhoId > 32 || valor.length < 2 + tamanhoId) {
			throw new ValorCifradoInvalidoException("Formato de valor cifrado desconhecido.");
		}
		return new String(valor, 2, tamanhoId, StandardCharsets.US_ASCII);
	}

	/**
	 * Cifra um texto com a chave ativa no formato textual do ADR 0025
	 * ({@code v1:<id>:<Base64 de IV e cifrado>}). {@code null} continua {@code null}.
	 */
	public String cifrarTexto(String claro) {
		if (claro == null) {
			return null;
		}
		String cabecalho = PREFIXO_TEXTO + chaveAtiva + ":";
		byte[] iv = novoIv();
		byte[] cifrado = executar(Cipher.ENCRYPT_MODE, chaves.get(chaveAtiva), iv,
				cabecalho.getBytes(StandardCharsets.US_ASCII), claro.getBytes(StandardCharsets.UTF_8));
		byte[] corpo = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
		return cabecalho + Base64.getEncoder().encodeToString(corpo);
	}

	/** Decifra um texto no formato textual do ADR 0025. {@code null} continua {@code null}. */
	public String decifrarTexto(String valor) {
		if (valor == null) {
			return null;
		}
		String id = chaveDoTexto(valor);
		String cabecalho = PREFIXO_TEXTO + id + ":";
		byte[] corpo;
		try {
			corpo = Base64.getDecoder().decode(valor.substring(cabecalho.length()));
		}
		catch (IllegalArgumentException erro) {
			throw new ValorCifradoInvalidoException("Valor cifrado com Base64 inválido.", erro);
		}
		if (corpo.length < TAMANHO_IV + TAMANHO_ETIQUETA_BITS / 8) {
			throw new ValorCifradoInvalidoException("Valor cifrado truncado.");
		}
		byte[] iv = Arrays.copyOfRange(corpo, 0, TAMANHO_IV);
		byte[] cifrado = Arrays.copyOfRange(corpo, TAMANHO_IV, corpo.length);
		byte[] claro = executar(Cipher.DECRYPT_MODE, chave(id), iv, cabecalho.getBytes(StandardCharsets.US_ASCII),
				cifrado);
		return new String(claro, StandardCharsets.UTF_8);
	}

	/** Identificador da chave que cifrou um texto. */
	public String chaveDoTexto(String valor) {
		if (valor == null || !valor.startsWith(PREFIXO_TEXTO)) {
			throw new ValorCifradoInvalidoException("Formato de valor cifrado desconhecido.");
		}
		int fimId = valor.indexOf(':', PREFIXO_TEXTO.length());
		if (fimId < 0) {
			throw new ValorCifradoInvalidoException("Formato de valor cifrado desconhecido.");
		}
		return valor.substring(PREFIXO_TEXTO.length(), fimId);
	}

	/** Diz se o valor foi cifrado com uma chave que não é a ativa e deve ser recifrado. */
	public boolean precisaRecifrar(String valor) {
		return !chaveAtiva.equals(chaveDoTexto(valor));
	}

	private SecretKey chave(String id) {
		SecretKey chave = chaves.get(id);
		if (chave == null) {
			throw new ValorCifradoInvalidoException(
					"A chave '" + id + "' que cifrou o valor não está em sgi.criptografia.chaves.");
		}
		return chave;
	}

	private static byte[] cabecalhoBinario(String id) {
		byte[] bytesId = id.getBytes(StandardCharsets.US_ASCII);
		return ByteBuffer.allocate(2 + bytesId.length).put(VERSAO).put((byte) bytesId.length).put(bytesId).array();
	}

	private byte[] novoIv() {
		byte[] iv = new byte[TAMANHO_IV];
		aleatorio.nextBytes(iv);
		return iv;
	}

	private static byte[] executar(int modo, SecretKey chave, byte[] iv, byte[] dadosAssociados, byte[] entrada) {
		try {
			Cipher cifra = Cipher.getInstance(TRANSFORMACAO);
			cifra.init(modo, chave, new GCMParameterSpec(TAMANHO_ETIQUETA_BITS, iv));
			cifra.updateAAD(dadosAssociados);
			return cifra.doFinal(entrada);
		}
		catch (AEADBadTagException erro) {
			throw new ValorCifradoInvalidoException("O valor cifrado foi adulterado ou a chave não confere.", erro);
		}
		catch (GeneralSecurityException erro) {
			throw new IllegalStateException("Falha na criptografia AES-GCM", erro);
		}
	}

}
