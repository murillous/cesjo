package br.org.cesjo.sgi.listaespera;

/**
 * Situação de uma inscrição na lista de espera (RF002). Só as inscrições {@link #AGUARDANDO} ocupam
 * posição na fila.
 */
public enum SituacaoInscricao {

	// PENDENTE-CESJO(P4): as situações de saída (desistência, não localizado, fora do perfil, inserido)
	// entram com a issue #29, quando a lista de motivos for decidida.

	AGUARDANDO("Aguardando");

	private final String rotulo;

	SituacaoInscricao(String rotulo) {
		this.rotulo = rotulo;
	}

	public String rotulo() {
		return rotulo;
	}

}
