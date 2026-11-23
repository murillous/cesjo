package br.org.cesjo.sgi.importacao.interno;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/** Entrada e saída de arquivos dos controladores de importação. */
final class ArquivosHttp {

	static final MediaType XLSX = MediaType
		.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private ArquivosHttp() {
	}

	static ResponseEntity<byte[]> planilha(String nome, byte[] conteudo) {
		return ResponseEntity.ok()
			.contentType(XLSX)
			.contentLength(conteudo.length)
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment().filename(nome, StandardCharsets.UTF_8).build().toString())
			.cacheControl(CacheControl.noStore())
			.body(conteudo);
	}

	/**
	 * Bytes do arquivo enviado. Confere o tamanho antes de ler, para não carregar na memória um arquivo
	 * acima do limite da importação (ADR 0218).
	 */
	static byte[] conteudo(MultipartFile arquivo) {
		if (arquivo.getSize() > LeitorPlanilha.TAMANHO_MAXIMO) {
			throw new RegraNegocioException("A planilha passa do limite de 2 MB. Divida em arquivos menores.");
		}
		try {
			return arquivo.getBytes();
		}
		catch (IOException erro) {
			throw new UncheckedIOException("Falha ao ler o arquivo enviado", erro);
		}
	}

}
