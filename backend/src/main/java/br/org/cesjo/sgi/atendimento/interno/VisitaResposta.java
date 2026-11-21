package br.org.cesjo.sgi.atendimento.interno;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.org.cesjo.sgi.institucional.ResumoColaborador;

/**
 * Visita como a API devolve, com o nome da pessoa e os profissionais em ordem de nome. Nunca leva as
 * observações, que têm leitura própria e auditada (ADR 0073); só diz se elas existem.
 *
 * @param visitaOrigemId visita cancelada que esta reagenda
 * @param reagendamentoId visita nova que reagenda esta
 */
record VisitaResposta(UUID id, UUID pessoaId, String nomePessoa, LocalDateTime dataHora, StatusVisita status,
		List<ResumoColaborador> profissionais, String motivoCancelamento, boolean temObservacoes,
		UUID visitaOrigemId, UUID reagendamentoId, Instant criadoEm) {

	static VisitaResposta de(VisitaDomiciliar visita, String nomePessoa, Map<UUID, ResumoColaborador> colaboradores,
			UUID reagendamentoId) {
		List<ResumoColaborador> profissionais = visita.getProfissionais()
			.stream()
			.map(colaboradores::get)
			.filter(java.util.Objects::nonNull)
			.sorted(Comparator.comparing(ResumoColaborador::nome))
			.toList();
		boolean temObservacoes = visita.getObservacoes() != null && !visita.getObservacoes().isBlank();
		return new VisitaResposta(visita.getId(), visita.getPessoaId(), nomePessoa, visita.getDataHora(),
				visita.getStatus(), profissionais, visita.getMotivoCancelamento(), temObservacoes,
				visita.getVisitaOrigemId(), reagendamentoId, visita.getCriadoEm());
	}

}
