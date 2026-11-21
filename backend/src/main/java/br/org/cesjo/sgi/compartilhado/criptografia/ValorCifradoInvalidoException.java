package br.org.cesjo.sgi.compartilhado.criptografia;

/**
 * Valor cifrado que não pode ser decifrado: formato desconhecido, chave ausente na configuração ou
 * conteúdo adulterado (a etiqueta do GCM não confere).
 */
public class ValorCifradoInvalidoException extends RuntimeException {

	public ValorCifradoInvalidoException(String mensagem) {
		super(mensagem);
	}

	public ValorCifradoInvalidoException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}

}
