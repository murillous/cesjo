package br.org.cesjo.sgi.arquivos;

import java.util.UUID;

import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;

/** Arquivo inexistente. A API responde 404. */
public class ArquivoNaoEncontradoException extends RecursoNaoEncontradoException {

	public ArquivoNaoEncontradoException(UUID id) {
		super("Arquivo não encontrado.");
	}

}
