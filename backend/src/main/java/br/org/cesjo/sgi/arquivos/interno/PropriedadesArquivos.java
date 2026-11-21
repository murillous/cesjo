package br.org.cesjo.sgi.arquivos.interno;

import java.nio.file.Path;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Propriedades {@code sgi.arquivos.*} (ADR 0026 e ADR 0027).
 *
 * @param diretorio diretório onde ficam os arquivos cifrados; em produção, o volume próprio
 * ({@code SGI_ARQUIVOS_DIRETORIO})
 * @param tamanhoMaximo tamanho máximo de um arquivo em claro; o limite do multipart do Spring é
 * derivado dele no {@code application.properties}
 */
@ConfigurationProperties("sgi.arquivos")
public record PropriedadesArquivos(Path diretorio, @DefaultValue("10MB") DataSize tamanhoMaximo) {

	public PropriedadesArquivos {
		Objects.requireNonNull(diretorio, "Defina sgi.arquivos.diretorio (variável SGI_ARQUIVOS_DIRETORIO).");
	}

}
