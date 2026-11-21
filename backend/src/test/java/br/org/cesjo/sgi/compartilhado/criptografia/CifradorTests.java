package br.org.cesjo.sgi.compartilhado.criptografia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Testes de unidade do {@link Cifrador}: formato, rotação de chave e validação da configuração. */
class CifradorTests {

	private static final String CHAVE_A = Base64.getEncoder().encodeToString(bytes(32, 1));

	private static final String CHAVE_B = Base64.getEncoder().encodeToString(bytes(32, 2));

	private final Cifrador cifrador = new Cifrador(new PropriedadesCriptografia("a", Map.of("a", CHAVE_A)));

	@Test
	void textoVoltaIgualDepoisDeCifradoEDecifrado() {
		String cifrado = cifrador.cifrarTexto("Relato fictício com acentuação: ção, é, ü.");

		assertThat(cifrado).startsWith("v1:a:").doesNotContain("Relato");
		assertThat(cifrador.decifrarTexto(cifrado)).isEqualTo("Relato fictício com acentuação: ção, é, ü.");
	}

	@Test
	void bytesVoltamIguaisDepoisDeCifradosEDecifrados() {
		byte[] claro = "conteúdo de arquivo".getBytes(StandardCharsets.UTF_8);

		byte[] cifrado = cifrador.cifrar(claro);

		assertThat(cifrado[0]).isEqualTo(Cifrador.VERSAO);
		assertThat(cifrador.chaveDoValor(cifrado)).isEqualTo("a");
		assertThat(cifrador.decifrar(cifrado)).isEqualTo(claro);
	}

	@Test
	void mesmoTextoCifradoDuasVezesGeraValoresDiferentes() {
		assertThat(cifrador.cifrarTexto("igual")).isNotEqualTo(cifrador.cifrarTexto("igual"));
	}

	@Test
	void nuloContinuaNulo() {
		assertThat(cifrador.cifrarTexto(null)).isNull();
		assertThat(cifrador.decifrarTexto(null)).isNull();
	}

	@Test
	void textoVazioVoltaVazio() {
		assertThat(cifrador.decifrarTexto(cifrador.cifrarTexto(""))).isEmpty();
	}

	@Test
	void rotacaoDecifraComAChaveDoValorECifraComAAtiva() {
		String cifradoComA = cifrador.cifrarTexto("antigo");
		byte[] bytesComA = cifrador.cifrar(new byte[] { 1, 2, 3 });
		Cifrador depoisDaRotacao = new Cifrador(new PropriedadesCriptografia("b", Map.of("a", CHAVE_A, "b", CHAVE_B)));

		assertThat(depoisDaRotacao.decifrarTexto(cifradoComA)).isEqualTo("antigo");
		assertThat(depoisDaRotacao.decifrar(bytesComA)).containsExactly(1, 2, 3);
		assertThat(depoisDaRotacao.precisaRecifrar(cifradoComA)).isTrue();
		String novo = depoisDaRotacao.cifrarTexto("antigo");
		assertThat(depoisDaRotacao.chaveDoTexto(novo)).isEqualTo("b");
		assertThat(depoisDaRotacao.precisaRecifrar(novo)).isFalse();
	}

	@Test
	void valorDeChaveQueSaiuDaConfiguracaoNaoDecifra() {
		Cifrador soB = new Cifrador(new PropriedadesCriptografia("b", Map.of("b", CHAVE_B)));
		String cifradoComA = cifrador.cifrarTexto("antigo");

		assertThatThrownBy(() -> soB.decifrarTexto(cifradoComA)).isInstanceOf(ValorCifradoInvalidoException.class)
			.hasMessageContaining("'a'");
	}

	@Test
	void valorAdulteradoNaoDecifra() {
		byte[] cifrado = cifrador.cifrar("original".getBytes(StandardCharsets.UTF_8));
		cifrado[cifrado.length - 1] ^= 1;

		assertThatThrownBy(() -> cifrador.decifrar(cifrado)).isInstanceOf(ValorCifradoInvalidoException.class);
	}

	@Test
	void trocarOIdentificadorDaChaveNoValorNaoDecifra() {
		Cifrador duasChaves = new Cifrador(new PropriedadesCriptografia("a", Map.of("a", CHAVE_A, "b", CHAVE_A)));
		String cifrado = duasChaves.cifrarTexto("segredo");
		String comOutroId = "v1:b:" + cifrado.substring("v1:a:".length());

		assertThatThrownBy(() -> duasChaves.decifrarTexto(comOutroId))
			.isInstanceOf(ValorCifradoInvalidoException.class);
	}

	@Test
	void formatoDesconhecidoNaoDecifra() {
		assertThatThrownBy(() -> cifrador.decifrarTexto("texto em claro"))
			.isInstanceOf(ValorCifradoInvalidoException.class);
		assertThatThrownBy(() -> cifrador.decifrar(new byte[] { 9, 9, 9 }))
			.isInstanceOf(ValorCifradoInvalidoException.class);
	}

	@Test
	void recusaConfiguracaoSemChaveAtiva() {
		assertThatThrownBy(() -> new Cifrador(new PropriedadesCriptografia(null, Map.of("a", CHAVE_A))))
			.isInstanceOf(ChaveCriptografiaInvalidaException.class)
			.hasMessageContaining("sgi.criptografia.chave-ativa");
	}

	@Test
	void recusaChaveAtivaQueNaoEstaNasChaves() {
		assertThatThrownBy(() -> new Cifrador(new PropriedadesCriptografia("b", Map.of("a", CHAVE_A))))
			.isInstanceOf(ChaveCriptografiaInvalidaException.class)
			.hasMessageContaining("sgi.criptografia.chaves.b");
	}

	@Test
	void recusaChaveComTamanhoErrado() {
		String curta = Base64.getEncoder().encodeToString(bytes(16, 1));

		assertThatThrownBy(() -> new Cifrador(new PropriedadesCriptografia("a", Map.of("a", curta))))
			.isInstanceOf(ChaveCriptografiaInvalidaException.class)
			.hasMessageContaining("16 bytes")
			.hasMessageNotContaining(curta);
	}

	@Test
	void recusaChaveForaDeBase64() {
		assertThatThrownBy(() -> new Cifrador(new PropriedadesCriptografia("a", Map.of("a", "não é base64!"))))
			.isInstanceOf(ChaveCriptografiaInvalidaException.class)
			.hasMessageContaining("Base64");
	}

	@Test
	void recusaIdentificadorDeChaveInvalido() {
		assertThatThrownBy(() -> new Cifrador(new PropriedadesCriptografia("A:1", Map.of("A:1", CHAVE_A))))
			.isInstanceOf(ChaveCriptografiaInvalidaException.class)
			.hasMessageContaining("Identificador");
	}

	private static byte[] bytes(int tamanho, int semente) {
		byte[] bytes = new byte[tamanho];
		for (int i = 0; i < tamanho; i++) {
			bytes[i] = (byte) (i * 7 + semente);
		}
		return bytes;
	}

}
