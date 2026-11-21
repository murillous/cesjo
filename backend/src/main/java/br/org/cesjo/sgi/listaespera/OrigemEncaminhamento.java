package br.org.cesjo.sgi.listaespera;

/** Quem encaminhou o candidato para a lista de espera (RF001). */
public enum OrigemEncaminhamento {

	CRAS("CRAS"),

	CREAS("CREAS"),

	JUDICIARIO("Judiciário"),

	SAUDE("Saúde"),

	EDUCACAO("Educação"),

	DEMANDA_ESPONTANEA("Demanda espontânea");

	private final String rotulo;

	OrigemEncaminhamento(String rotulo) {
		this.rotulo = rotulo;
	}

	/** Nome exibido nas telas e nas exportações. */
	public String rotulo() {
		return rotulo;
	}

}
