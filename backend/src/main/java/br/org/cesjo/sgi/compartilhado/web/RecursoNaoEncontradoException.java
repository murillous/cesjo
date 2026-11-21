package br.org.cesjo.sgi.compartilhado.web;

/**
 * Exceção base para recurso inexistente. A API responde 404 em {@code ProblemDetail}.
 *
 * <p>Os módulos podem lançá-la direto ou estendê-la com uma exceção mais específica.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}

}
