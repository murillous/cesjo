package br.org.cesjo.sgi.compartilhado.web;

/**
 * Exceção base para violação de regra de negócio. A API responde 422 em {@code ProblemDetail}.
 *
 * <p>A mensagem vai para o cliente, então deve ser clara para quem usa o sistema e não pode conter
 * dado sensível.
 */
public class RegraNegocioException extends RuntimeException {

	public RegraNegocioException(String mensagem) {
		super(mensagem);
	}

}
