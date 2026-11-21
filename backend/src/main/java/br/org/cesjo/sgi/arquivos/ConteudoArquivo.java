package br.org.cesjo.sgi.arquivos;

/**
 * Arquivo decifrado, com seus metadados.
 *
 * @param metadados metadados do arquivo
 * @param conteudo bytes em claro
 */
public record ConteudoArquivo(MetadadosArquivo metadados, byte[] conteudo) {

}
