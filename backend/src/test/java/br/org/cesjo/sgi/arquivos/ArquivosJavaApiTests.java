package br.org.cesjo.sgi.arquivos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import br.org.cesjo.sgi.TestcontainersConfiguration;

/** API Java do módulo, usada pelos outros módulos sem passar pelo HTTP (ADR 0028). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ArquivosJavaApiTests {

	@Autowired
	Arquivos arquivos;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void guardaELeSemRequisicaoAutenticada() {
		byte[] pdf = ExemplosDeArquivo.pdf();

		MetadadosArquivo guardado = arquivos.guardar(new NovoArquivo("comprovante", pdf));

		assertThat(guardado.nomeOriginal()).isEqualTo("comprovante.pdf");
		assertThat(guardado.tipo()).isEqualTo(TipoArquivo.PDF);
		assertThat(guardado.enviadoPor()).isNull();
		assertThat(arquivos.metadados(guardado.id())).contains(guardado);
		ConteudoArquivo lido = arquivos.ler(guardado.id());
		assertThat(lido.conteudo()).isEqualTo(pdf);
		assertThat(lido.metadados()).isEqualTo(guardado);
	}

	@Test
	void recusaTipoNaoAceito() {
		assertThatThrownBy(() -> arquivos.guardar(new NovoArquivo("planilha.xlsx", new byte[] { 'P', 'K', 3, 4 })))
			.isInstanceOf(ArquivoRejeitadoException.class);
	}

	@Test
	void lerInexistenteLancaNaoEncontrado() {
		assertThatThrownBy(() -> arquivos.ler(UUID.randomUUID())).isInstanceOf(ArquivoNaoEncontradoException.class);
		assertThat(arquivos.metadados(UUID.randomUUID())).isEmpty();
	}

	@Test
	void conteudoAlteradoNoDiscoNaoELido() throws Exception {
		MetadadosArquivo guardado = arquivos.guardar(new NovoArquivo("alterado.png", ExemplosDeArquivo.png()));
		jdbc.update("update arquivo set sha256 = ? where id = ?", "0".repeat(64), guardado.id());

		assertThatThrownBy(() -> arquivos.ler(guardado.id())).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("SHA-256");
	}

}
