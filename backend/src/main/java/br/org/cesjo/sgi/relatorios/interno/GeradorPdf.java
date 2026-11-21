package br.org.cesjo.sgi.relatorios.interno;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import br.org.cesjo.sgi.relatorios.TabelaExportacao;

/**
 * Gera o PDF da tabela: modelo XHTML do Thymeleaf renderizado pelo openhtmltopdf (ADR 0040).
 *
 * <p>
 * O motor do Thymeleaf é próprio deste gerador, sem o starter web: o backend é só API REST e não
 * renderiza páginas.
 */
@Component
class GeradorPdf {

	/** Acima disso a página vira paisagem, para as colunas não ficarem estreitas demais. */
	static final int COLUNAS_MAXIMAS_EM_RETRATO = 6;

	private static final String MODELO = "tabela";

	private final TemplateEngine motor;

	private final String logo;

	private final Map<String, String> cores = Map.of("primaria", IdentidadeVisual.PRIMARIA, "primariaEscura",
			IdentidadeVisual.PRIMARIA_ESCURA, "fundoSecundario", IdentidadeVisual.FUNDO_SECUNDARIO, "borda",
			IdentidadeVisual.BORDA, "textoSecundario", IdentidadeVisual.TEXTO_SECUNDARIO);

	GeradorPdf() {
		ClassLoaderTemplateResolver resolvedor = new ClassLoaderTemplateResolver();
		resolvedor.setPrefix("relatorios/modelos/");
		resolvedor.setSuffix(".html");
		resolvedor.setTemplateMode(TemplateMode.XML);
		resolvedor.setCharacterEncoding("UTF-8");
		resolvedor.setCacheable(true);
		this.motor = new TemplateEngine();
		this.motor.setTemplateResolver(resolvedor);
		this.logo = "data:image/png;base64," + Base64.getEncoder().encodeToString(lerLogo());
	}

	byte[] gerar(TabelaExportacao tabela, Instant geradoEm) {
		Context contexto = new Context(IdentidadeVisual.IDIOMA);
		contexto.setVariable("tabela", tabela);
		contexto.setVariable("linhas", textos(tabela.linhas()));
		contexto.setVariable("geradoEm", FormatacaoCelula.dataHora(geradoEm));
		contexto.setVariable("paisagem", tabela.colunas().size() > COLUNAS_MAXIMAS_EM_RETRATO);
		contexto.setVariable("logo", logo);
		contexto.setVariable("cores", cores);
		String html = motor.process(MODELO, contexto);

		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		try {
			new PdfRendererBuilder().useFastMode()
				.withHtmlContent(html, null)
				// Só o logo embutido: nenhum dado exportado faz o renderizador buscar recurso fora.
				.useExternalResourceAccessControl((uri, tipo) -> uri != null && uri.startsWith("data:"),
						ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI)
				.useExternalResourceAccessControl((uri, tipo) -> uri != null && uri.startsWith("data:"),
						ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI)
				.toStream(saida)
				.run();
		}
		catch (IOException erro) {
			throw new UncheckedIOException("Falha ao gerar o PDF da exportação", erro);
		}
		return saida.toByteArray();
	}

	private static List<List<String>> textos(List<List<Object>> linhas) {
		return linhas.stream().map(linha -> linha.stream().map(FormatacaoCelula::texto).toList()).toList();
	}

	private static byte[] lerLogo() {
		try (InputStream entrada = new ClassPathResource(IdentidadeVisual.LOGO).getInputStream()) {
			return entrada.readAllBytes();
		}
		catch (IOException erro) {
			throw new UncheckedIOException("Logo da exportação não encontrado: " + IdentidadeVisual.LOGO, erro);
		}
	}

}
