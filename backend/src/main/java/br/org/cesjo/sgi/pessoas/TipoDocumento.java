package br.org.cesjo.sgi.pessoas;

/**
 * Tipos de documento anexado a uma pessoa (tabela {@code documento_pessoa}, ADR 0055). Uma pessoa tem
 * no máximo um documento de cada tipo; um envio novo substitui o anterior.
 */
public enum TipoDocumento {

	LAUDO_MEDICO("Laudo médico"),

	COMPROVANTE_RESIDENCIA("Comprovante de residência");

	private final String rotulo;

	TipoDocumento(String rotulo) {
		this.rotulo = rotulo;
	}

	/** Nome exibido nas telas. */
	public String rotulo() {
		return rotulo;
	}

}
