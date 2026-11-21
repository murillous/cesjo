package br.org.cesjo.sgi.arquivos;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadados de um arquivo guardado. O conteúdo fica cifrado no diretório de arquivos, nunca no banco.
 *
 * @param id identificador do arquivo, usado pelos outros módulos para referenciá-lo
 * @param nomeOriginal nome normalizado, sempre com a extensão do tipo reconhecido
 * @param tipo tipo reconhecido pelo conteúdo
 * @param tamanho tamanho do conteúdo em claro, em bytes
 * @param sha256 SHA-256 do conteúdo em claro, em hexadecimal minúsculo
 * @param enviadoPor conta que enviou, vazio quando o envio não veio de uma requisição autenticada
 * @param enviadoEm momento do envio
 */
public record MetadadosArquivo(UUID id, String nomeOriginal, TipoArquivo tipo, long tamanho, String sha256,
		UUID enviadoPor, Instant enviadoEm) {

}
