package br.org.cesjo.sgi.pessoas;

/**
 * Regra do telefone de contato (ADR 0038): DDD mais número, 10 ou 11 dígitos, aceito com ou sem
 * máscara e guardado só com os dígitos.
 */
public final class Telefone {

	/**
	 * Expressão para {@code @Pattern}: só dígitos, espaço, parênteses, hífen, ponto e {@code +}, com 10 ou
	 * 11 dígitos no total.
	 */
	public static final String FORMATO = "^[\\s().+-]*(\\d[\\s().+-]*){10,11}$";

	public static final String MENSAGEM = "informe DDD e número, com 10 ou 11 dígitos";

	private Telefone() {
	}

	public static String normalizar(String telefone) {
		return telefone.replaceAll("\\D", "");
	}

}
