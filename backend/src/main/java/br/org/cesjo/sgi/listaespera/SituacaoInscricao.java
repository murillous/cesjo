package br.org.cesjo.sgi.listaespera;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situação de uma inscrição na lista de espera (RF002, ADR 0091). As inscrições {@link #AGUARDANDO} e
 * {@link #EM_AVALIACAO} são de candidatos ativos: ocupam posição na fila, pela data de cadastro
 * (ADR 0049 e ADR 0094).
 */
public enum SituacaoInscricao {

	AGUARDANDO("Aguardando"),

	// PENDENTE-CESJO(P4): o candidato em avaliação continua na fila, na mesma ordem, e não volta para
	// aguardando quando a visita é cancelada (ADR 0049).
	/** Candidato com visita domiciliar de avaliação agendada (RF004). */
	EM_AVALIACAO("Em avaliação"),

	/** Saiu da fila porque foi inserida no serviço (RF007, ADR 0081). */
	INSERIDO("Inserido no serviço"),

	// PENDENTE-CESJO(P4): a saída da lista é uma situação só, com o motivo em MotivoSaida (ADR 0091).
	/** Saiu da lista sem ser inserida, com um {@link MotivoSaida}. */
	SAIU("Saiu da lista");

	/** Situações de quem ainda é candidato, isto é, segue na lista de espera. */
	public static final Set<SituacaoInscricao> ATIVAS = EnumSet.of(AGUARDANDO, EM_AVALIACAO);

	private final String rotulo;

	SituacaoInscricao(String rotulo) {
		this.rotulo = rotulo;
	}

	public String rotulo() {
		return rotulo;
	}

	/** Diz se a inscrição segue na fila, com posição. */
	public boolean ativa() {
		return ATIVAS.contains(this);
	}

}
