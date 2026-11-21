package br.org.cesjo.sgi.arquivos;

import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/** Arquivo recusado no envio: vazio, grande demais ou de tipo não aceito. A API responde 422. */
public class ArquivoRejeitadoException extends RegraNegocioException {

	public ArquivoRejeitadoException(String mensagem) {
		super(mensagem);
	}

}
