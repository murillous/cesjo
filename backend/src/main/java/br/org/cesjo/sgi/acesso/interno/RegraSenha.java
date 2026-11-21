package br.org.cesjo.sgi.acesso.interno;

import java.security.SecureRandom;
import java.util.Locale;

import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Regra de senha (ADR 0012): tamanho mínimo configurável, máximo de 128 caracteres, sem regra de
 * composição, diferente do login. As senhas temporárias são geradas aqui.
 */
@Component
class RegraSenha {

	static final int TAMANHO_MAXIMO = 128;

	static final int TAMANHO_TEMPORARIA = 16;

	/** Sem caracteres que se confundem na leitura (0/O, 1/l/I). */
	private static final String ALFABETO_TEMPORARIA = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	private final SecureRandom aleatorio = new SecureRandom();

	private final int tamanhoMinimo;

	RegraSenha(PropriedadesAcesso propriedades) {
		this.tamanhoMinimo = propriedades.senhaTamanhoMinimo();
	}

	int tamanhoMinimo() {
		return tamanhoMinimo;
	}

	/** Lança {@link RegraNegocioException} com a mensagem para a tela quando a senha não serve. */
	void validar(String senha, String login) {
		int tamanho = senha == null ? 0 : senha.codePointCount(0, senha.length());
		if (tamanho < tamanhoMinimo) {
			throw new RegraNegocioException("A senha precisa ter pelo menos " + tamanhoMinimo + " caracteres.");
		}
		if (tamanho > TAMANHO_MAXIMO) {
			throw new RegraNegocioException("A senha pode ter no máximo " + TAMANHO_MAXIMO + " caracteres.");
		}
		if (senha.isBlank()) {
			throw new RegraNegocioException("A senha não pode ser só espaços.");
		}
		if (login != null && senha.strip().toLowerCase(Locale.ROOT).equals(login.toLowerCase(Locale.ROOT))) {
			throw new RegraNegocioException("A senha não pode ser igual ao login.");
		}
	}

	String gerarTemporaria() {
		StringBuilder senha = new StringBuilder(TAMANHO_TEMPORARIA);
		for (int i = 0; i < TAMANHO_TEMPORARIA; i++) {
			senha.append(ALFABETO_TEMPORARIA.charAt(aleatorio.nextInt(ALFABETO_TEMPORARIA.length())));
		}
		return senha.toString();
	}

}
