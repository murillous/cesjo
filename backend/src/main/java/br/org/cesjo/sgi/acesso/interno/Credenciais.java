package br.org.cesjo.sgi.acesso.interno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Login e senha informados na tela de entrada. */
record Credenciais(@NotBlank @Size(max = 60) String login, @NotBlank @Size(max = RegraSenha.TAMANHO_MAXIMO) String senha) {

	@Override
	public String toString() {
		return "Credenciais[login=" + login + "]";
	}

}
