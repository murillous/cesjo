package br.org.cesjo.sgi.atendimento.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.institucional.ResumoColaborador;

/** Escuta completa, com os textos decifrados. Só sai pela leitura auditada (ADR 0100). */
record EscutaResposta(UUID id, UUID pessoaId, String nomePessoa, LocalDate dataEscuta, ResumoColaborador profissional,
		String dinamicaFamiliar, String necessidadesImediatas, String habilidades, String potencialidades,
		Instant criadoEm, Instant atualizadoEm) {

	static EscutaResposta de(Escuta escuta, String nomePessoa, ResumoColaborador profissional) {
		return new EscutaResposta(escuta.getId(), escuta.getPessoaId(), nomePessoa, escuta.getDataEscuta(),
				profissional, escuta.getDinamicaFamiliar(), escuta.getNecessidadesImediatas(), escuta.getHabilidades(),
				escuta.getPotencialidades(), escuta.getCriadoEm(), escuta.getAtualizadoEm());
	}

}
