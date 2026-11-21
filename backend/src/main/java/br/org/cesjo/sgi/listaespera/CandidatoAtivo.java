package br.org.cesjo.sgi.listaespera;

import java.time.LocalDate;
import java.util.UUID;

/** Candidato que segue na lista de espera, como os outros módulos o veem (ADR 0049). */
public record CandidatoAtivo(UUID inscricaoId, UUID pessoaId, String nomeCompleto, LocalDate dataCadastro,
		SituacaoInscricao situacao) {
}
