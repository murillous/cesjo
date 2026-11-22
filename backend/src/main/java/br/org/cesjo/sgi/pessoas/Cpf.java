package br.org.cesjo.sgi.pessoas;

/** Regra do CPF do cadastro completo (ADR 0140): 11 dígitos com os dígitos verificadores certos. */
public final class Cpf {

	public static final String MENSAGEM = "CPF inválido";

	private Cpf() {
	}

	/** Diz se o CPF, com ou sem máscara, é válido. Nulo ou em branco conta como válido (campo opcional). */
	public static boolean valido(String cpf) {
		if (cpf == null || cpf.isBlank()) {
			return true;
		}
		if (!cpf.matches("[\\d.\\s-]+")) {
			return false;
		}
		String digitos = cpf.replaceAll("\\D", "");
		if (digitos.length() != 11 || digitos.chars().distinct().count() == 1) {
			return false;
		}
		return digito(digitos, 9) == digitos.charAt(9) - '0' && digito(digitos, 10) == digitos.charAt(10) - '0';
	}

	private static int digito(String digitos, int posicao) {
		int soma = 0;
		for (int i = 0; i < posicao; i++) {
			soma += (digitos.charAt(i) - '0') * (posicao + 1 - i);
		}
		int resto = soma * 10 % 11;
		return resto == 10 ? 0 : resto;
	}

}
