package br.org.cesjo.sgi.acesso.interno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Troca de senha pela própria conta. A regra de tamanho fica em {@link RegraSenha}. */
record TrocaSenha(@NotBlank String senhaAtual, @NotNull String novaSenha) {

	@Override
	public String toString() {
		return "TrocaSenha[]";
	}

}
