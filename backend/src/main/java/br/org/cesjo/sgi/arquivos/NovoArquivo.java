package br.org.cesjo.sgi.arquivos;

import java.util.Objects;

/**
 * Arquivo a guardar.
 *
 * @param nomeOriginal nome informado por quem enviou; pode ter caminho, que é descartado
 * @param conteudo bytes do arquivo
 */
public record NovoArquivo(String nomeOriginal, byte[] conteudo) {

	public NovoArquivo {
		Objects.requireNonNull(conteudo, "conteudo");
	}

}
