package br.org.cesjo.sgi.arquivos.interno;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.org.cesjo.sgi.arquivos.TipoArquivo;

/** Normalização do nome guardado (ADR 0027). */
class ServicoArquivosTests {

	@Test
	void mantemNomeComExtensaoDoTipo() {
		assertThat(ServicoArquivos.normalizarNome("Laudo Médico.PDF", TipoArquivo.PDF)).isEqualTo("Laudo Médico.PDF");
		assertThat(ServicoArquivos.normalizarNome("foto.jpeg", TipoArquivo.JPEG)).isEqualTo("foto.jpeg");
	}

	@Test
	void acrescentaExtensaoQuandoNaoConfereComOConteudo() {
		assertThat(ServicoArquivos.normalizarNome("programa.exe", TipoArquivo.PDF)).isEqualTo("programa.exe.pdf");
		assertThat(ServicoArquivos.normalizarNome("imagem.png", TipoArquivo.JPEG)).isEqualTo("imagem.png.jpg");
	}

	@Test
	void descartaCaminhoECaracteresDeControle() {
		assertThat(ServicoArquivos.normalizarNome("C:\\pasta\\..\\doc.pdf", TipoArquivo.PDF)).isEqualTo("doc.pdf");
		assertThat(ServicoArquivos.normalizarNome("../../etc/doc\n.pdf", TipoArquivo.PDF)).isEqualTo("doc.pdf");
	}

	@Test
	void usaNomePadraoQuandoVazio() {
		assertThat(ServicoArquivos.normalizarNome(null, TipoArquivo.PNG)).isEqualTo("arquivo.png");
		assertThat(ServicoArquivos.normalizarNome("  ", TipoArquivo.PDF)).isEqualTo("arquivo.pdf");
	}

	@Test
	void limitaOTamanhoMantendoAExtensao() {
		String nome = ServicoArquivos.normalizarNome("a".repeat(400) + ".pdf", TipoArquivo.PDF);

		assertThat(nome).hasSize(ServicoArquivos.TAMANHO_MAXIMO_NOME).endsWith(".pdf");
	}

}
