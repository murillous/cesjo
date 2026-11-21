package br.org.cesjo.sgi.listaespera.interno;

import java.time.Instant;
import java.time.LocalDate;

import br.org.cesjo.sgi.listaespera.MotivoSaida;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;

/** Uma mudança de situação, como a ficha mostra. */
record HistoricoResposta(SituacaoInscricao situacaoAnterior, SituacaoInscricao situacaoNova, LocalDate data,
		MotivoSaida motivoSaida, String observacao, Instant registradoEm) {

	static HistoricoResposta de(HistoricoSituacao historico) {
		return new HistoricoResposta(historico.getSituacaoAnterior(), historico.getSituacaoNova(), historico.getData(),
				historico.getMotivoSaida(), historico.getObservacao(), historico.getRegistradoEm());
	}

}
