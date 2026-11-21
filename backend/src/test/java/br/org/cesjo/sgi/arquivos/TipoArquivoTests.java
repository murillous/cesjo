package br.org.cesjo.sgi.arquivos;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TipoArquivoTests {

	@Test
	void reconhecePelaAssinaturaDosBytes() {
		assertThat(TipoArquivo.detectar(ExemplosDeArquivo.pdf())).contains(TipoArquivo.PDF);
		assertThat(TipoArquivo.detectar(ExemplosDeArquivo.jpeg())).contains(TipoArquivo.JPEG);
		assertThat(TipoArquivo.detectar(ExemplosDeArquivo.png())).contains(TipoArquivo.PNG);
	}

	@Test
	void recusaOutrosConteudos() {
		assertThat(TipoArquivo.detectar("texto simples".getBytes(StandardCharsets.UTF_8))).isEmpty();
		assertThat(TipoArquivo.detectar(new byte[] { 'G', 'I', 'F', '8', '9', 'a' })).isEmpty();
		assertThat(TipoArquivo.detectar(new byte[] { 'M', 'Z' })).isEmpty();
		assertThat(TipoArquivo.detectar(new byte[] { '%', 'P', 'D' })).isEmpty();
		assertThat(TipoArquivo.detectar(new byte[0])).isEmpty();
		assertThat(TipoArquivo.detectar(null)).isEmpty();
	}

}
