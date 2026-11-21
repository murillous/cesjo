package br.org.cesjo.sgi.compartilhado.criptografia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * A aplicação não sobe sem chave ativa válida (ADR 0004). Sobe só a configuração da criptografia, com
 * as propriedades como viriam do ambiente, e confere que o contexto falha com mensagem clara.
 */
class InicializacaoSemChaveTests {

	private static final String CHAVE = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";

	private final ApplicationContextRunner contexto = new ApplicationContextRunner()
		.withUserConfiguration(CriptografiaConfig.class, Cifrador.class);

	@Test
	void naoSobeSemChaveAtiva() {
		contexto.run(aplicacao -> assertThat(aplicacao).hasFailed()
			.getFailure()
			.rootCause()
			.isInstanceOf(ChaveCriptografiaInvalidaException.class)
			.hasMessageContaining("Chave de criptografia ativa não configurada"));
	}

	@Test
	void naoSobeComChaveAtivaSemValor() {
		contexto.withPropertyValues("sgi.criptografia.chave-ativa=prod1")
			.run(aplicacao -> assertThat(aplicacao).hasFailed()
				.getFailure()
				.rootCause()
				.isInstanceOf(ChaveCriptografiaInvalidaException.class)
				.hasMessageContaining("SGI_CRIPTOGRAFIA_CHAVES_PROD1"));
	}

	@Test
	void naoSobeComChaveDeTamanhoErrado() {
		contexto.withPropertyValues("sgi.criptografia.chave-ativa=prod1", "sgi.criptografia.chaves.prod1=AAEC")
			.run(aplicacao -> assertThat(aplicacao).hasFailed()
				.getFailure()
				.rootCause()
				.isInstanceOf(ChaveCriptografiaInvalidaException.class)
				.hasMessageContaining("bytes"));
	}

	@Test
	void sobeComChaveAtivaValida() {
		contexto.withPropertyValues("sgi.criptografia.chave-ativa=prod1", "sgi.criptografia.chaves.prod1=" + CHAVE)
			.run(aplicacao -> {
				assertThat(aplicacao).hasNotFailed();
				assertThat(aplicacao.getBean(Cifrador.class).chaveAtiva()).isEqualTo("prod1");
			});
	}

}
