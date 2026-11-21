package br.org.cesjo.sgi.acesso.interno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.acesso.Perfil;

/**
 * Dados para criar uma conta. O login aceita letras minúsculas sem acento, números, ponto, hífen e
 * sublinhado, de 3 a 60 caracteres (ADR 0013); maiúsculas são convertidas antes de gravar.
 */
record NovaConta(
		@NotBlank @Pattern(regexp = "^\s*[A-Za-z0-9._-]{3,60}\s*$",
				message = "use de 3 a 60 caracteres entre letras sem acento, números, ponto, hífen e sublinhado") String login,
		@NotBlank @Size(max = 150) String nome, @NotNull Perfil perfil) {
}
