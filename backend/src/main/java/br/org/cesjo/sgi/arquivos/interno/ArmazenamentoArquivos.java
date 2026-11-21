package br.org.cesjo.sgi.arquivos.interno;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Grava e lê os arquivos já cifrados no diretório configurado (ADR 0026). O caminho guardado no banco é
 * relativo ao diretório: {@code <dois primeiros caracteres do id>/<id>}. O nome original nunca vai para
 * o disco.
 */
@Component
class ArmazenamentoArquivos {

	private final Path raiz;

	ArmazenamentoArquivos(PropriedadesArquivos propriedades) {
		this.raiz = propriedades.diretorio().toAbsolutePath().normalize();
		try {
			Files.createDirectories(raiz);
		}
		catch (IOException erro) {
			throw new IllegalStateException("Não foi possível criar o diretório de arquivos " + raiz
					+ ". Confira sgi.arquivos.diretorio (variável SGI_ARQUIVOS_DIRETORIO).", erro);
		}
		if (!Files.isWritable(raiz)) {
			throw new IllegalStateException("Sem permissão de escrita no diretório de arquivos " + raiz
					+ ". Confira sgi.arquivos.diretorio (variável SGI_ARQUIVOS_DIRETORIO).");
		}
	}

	/** Caminho relativo onde o arquivo do id fica guardado. */
	static String caminhoDe(UUID id) {
		String texto = id.toString();
		return texto.substring(0, 2) + "/" + texto;
	}

	/** Grava os bytes cifrados de forma atômica: escreve num temporário e renomeia. */
	void gravar(String caminho, byte[] cifrado) {
		Path destino = resolver(caminho);
		try {
			Files.createDirectories(destino.getParent());
			Path temporario = Files.createTempFile(destino.getParent(), destino.getFileName().toString(), ".tmp");
			try {
				Files.write(temporario, cifrado);
				Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE);
			}
			finally {
				Files.deleteIfExists(temporario);
			}
		}
		catch (IOException erro) {
			throw new UncheckedIOException("Falha ao gravar o arquivo " + caminho, erro);
		}
	}

	byte[] ler(String caminho) {
		try {
			return Files.readAllBytes(resolver(caminho));
		}
		catch (IOException erro) {
			throw new UncheckedIOException("Falha ao ler o arquivo " + caminho, erro);
		}
	}

	void apagar(String caminho) {
		try {
			Files.deleteIfExists(resolver(caminho));
		}
		catch (IOException erro) {
			throw new UncheckedIOException("Falha ao apagar o arquivo " + caminho, erro);
		}
	}

	Path resolver(String caminho) {
		Path resolvido = raiz.resolve(caminho).normalize();
		if (!resolvido.startsWith(raiz)) {
			throw new IllegalArgumentException("Caminho fora do diretório de arquivos: " + caminho);
		}
		return resolvido;
	}

}
