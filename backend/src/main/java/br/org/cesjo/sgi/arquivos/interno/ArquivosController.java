package br.org.cesjo.sgi.arquivos.interno;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.org.cesjo.sgi.arquivos.Arquivos;
import br.org.cesjo.sgi.arquivos.ConteudoArquivo;
import br.org.cesjo.sgi.arquivos.MetadadosArquivo;
import br.org.cesjo.sgi.arquivos.NovoArquivo;

/** Envio e download de arquivos (ADR 0028). A auditoria fica no {@link Arquivos}. */
@RestController
@RequestMapping("/api/arquivos")
class ArquivosController {

	private final Arquivos arquivos;

	ArquivosController(Arquivos arquivos) {
		this.arquivos = arquivos;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('DOCUMENTOS_ENVIAR')")
	ResponseEntity<ArquivoResposta> enviar(@RequestPart("arquivo") MultipartFile arquivo) {
		MetadadosArquivo metadados = arquivos.guardar(new NovoArquivo(arquivo.getOriginalFilename(), bytes(arquivo)));
		return ResponseEntity.created(URI.create("/api/arquivos/" + metadados.id()))
			.body(ArquivoResposta.de(metadados));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('DOCUMENTOS_LER')")
	ResponseEntity<byte[]> baixar(@PathVariable UUID id) {
		ConteudoArquivo arquivo = arquivos.ler(id);
		MetadadosArquivo metadados = arquivo.metadados();
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(metadados.tipo().tipoMime()))
			.contentLength(arquivo.conteudo().length)
			.header("Content-Disposition",
					ContentDisposition.attachment()
						.filename(metadados.nomeOriginal(), StandardCharsets.UTF_8)
						.build()
						.toString())
			.cacheControl(CacheControl.noStore())
			.body(arquivo.conteudo());
	}

	private static byte[] bytes(MultipartFile arquivo) {
		try {
			return arquivo.getBytes();
		}
		catch (IOException erro) {
			throw new UncheckedIOException("Falha ao ler o arquivo enviado", erro);
		}
	}

}
