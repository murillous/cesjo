package br.org.cesjo.sgi.relatorios;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Arquivo gerado por {@link Exportacao}, pronto para download.
 *
 * @param nomeArquivo nome sugerido para o download, com a extensão
 * @param formato formato do conteúdo
 * @param conteudo bytes do PDF ou da planilha
 */
public record ArquivoExportado(String nomeArquivo, FormatoExportacao formato, byte[] conteudo) {

	public ArquivoExportado {
		Objects.requireNonNull(nomeArquivo, "nomeArquivo");
		Objects.requireNonNull(formato, "formato");
		Objects.requireNonNull(conteudo, "conteudo");
	}

	/**
	 * Resposta HTTP padrão de download de exportação (ADR 0042): tipo do formato, anexo com o nome em
	 * UTF-8 e sem cache, porque o conteúdo tem dado pessoal.
	 */
	public ResponseEntity<byte[]> comoDownload() {
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(formato.tipoMime()))
			.contentLength(conteudo.length)
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment().filename(nomeArquivo, StandardCharsets.UTF_8).build().toString())
			.cacheControl(CacheControl.noStore())
			.body(conteudo);
	}

}
