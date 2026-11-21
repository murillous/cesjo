package br.org.cesjo.sgi.acesso;

import org.springframework.security.core.Authentication;

/**
 * Autenticação de uma conta de teste para usar numa requisição só, com
 * {@code SecurityMockMvcRequestPostProcessors.authentication(...)}, quando o teste precisa agir como
 * outro perfil além do de {@link ComConta}.
 */
public final class AutenticacaoDeTeste {

	private AutenticacaoDeTeste() {
	}

	public static Authentication de(Perfil perfil, String login) {
		ComConta anotacao = new ComConta() {

			@Override
			public Class<? extends java.lang.annotation.Annotation> annotationType() {
				return ComConta.class;
			}

			@Override
			public Perfil perfil() {
				return perfil;
			}

			@Override
			public String id() {
				return "";
			}

			@Override
			public String login() {
				return login;
			}

		};
		return new ComContaFactory().createSecurityContext(anotacao).getAuthentication();
	}

}
