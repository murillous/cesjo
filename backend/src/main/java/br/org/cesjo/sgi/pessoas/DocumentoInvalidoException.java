package br.org.cesjo.sgi.pessoas;

import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/** Arquivo que não pode ser anexado: inexistente ou já ligado a outro documento. A API responde 422. */
public class DocumentoInvalidoException extends RegraNegocioException {

	public DocumentoInvalidoException(String mensagem) {
		super(mensagem);
	}

}
