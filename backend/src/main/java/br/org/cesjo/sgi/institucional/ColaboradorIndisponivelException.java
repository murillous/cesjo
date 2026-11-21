package br.org.cesjo.sgi.institucional;

import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Um ou mais colaboradores informados não existem ou estão inativos. A API HTTP de quem chama responde
 * 422.
 */
public class ColaboradorIndisponivelException extends RegraNegocioException {

	public ColaboradorIndisponivelException() {
		super("Escolha apenas colaboradores ativos.");
	}

}
