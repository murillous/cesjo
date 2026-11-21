package br.org.cesjo.sgi.pessoas;

import java.time.Instant;
import java.util.UUID;

/**
 * Documento anexado a uma pessoa, com os metadados do arquivo e sem o conteúdo.
 *
 * @param id identificador do documento
 * @param tipo tipo do documento
 * @param arquivoId arquivo guardado no módulo Arquivos
 * @param nomeArquivo nome do arquivo, como foi enviado e normalizado
 * @param tipoMime tipo do arquivo reconhecido pelo conteúdo
 * @param tamanho tamanho em bytes
 * @param anexadoEm momento em que o arquivo atual foi ligado à pessoa
 */
public record DocumentoPessoa(UUID id, TipoDocumento tipo, UUID arquivoId, String nomeArquivo, String tipoMime,
		long tamanho, Instant anexadoEm) {
}
