package br.org.cesjo.sgi.compartilhado.criptografia;

/**
 * Configuração de chaves ausente ou inválida. Lançada na inicialização, impede a aplicação de subir
 * (ADR 0004). A mensagem nunca contém o valor de uma chave.
 */
public class ChaveCriptografiaInvalidaException extends IllegalStateException {

	public ChaveCriptografiaInvalidaException(String mensagem) {
		super(mensagem);
	}

}
