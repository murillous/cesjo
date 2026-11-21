package br.org.cesjo.sgi.listaespera;

/**
 * Situação de uma inscrição na lista de espera (RF002). As inscrições {@link #AGUARDANDO} e
 * {@link #EM_AVALIACAO} são de candidatos ativos e seguem na fila pela data de cadastro (ADR 0049).
 */
public enum SituacaoInscricao {

	// PENDENTE-CESJO(P4): as outras situações de saída (desistência, não localizado, fora do perfil)
	// entram com a issue #29, quando a lista de motivos for decidida.

	AGUARDANDO("Aguardando"),

	// PENDENTE-CESJO(P4): o candidato em avaliação continua na fila, na mesma ordem, e não volta para
	// aguardando quando a visita é cancelada (ADR 0049).
	/** Candidato com visita domiciliar de avaliação agendada (RF004). */
	EM_AVALIACAO("Em avaliação"),

	/** Saiu da fila porque foi inserida no serviço (RF007, ADR 0081). */
	INSERIDO("Inserido no serviço");

	/** Situações de quem ainda é candidato, isto é, segue na lista de espera. */
	public static final java.util.Set<SituacaoInscricao> ATIVAS = java.util.EnumSet.of(AGUARDANDO, EM_AVALIACAO);

	private final String rotulo;

	SituacaoInscricao(String rotulo) {
		this.rotulo = rotulo;
	}

	public String rotulo() {
		return rotulo;
	}

}
