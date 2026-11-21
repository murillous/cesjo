package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.atendidos.SituacaoInsercao;
import br.org.cesjo.sgi.atendidos.Turno;

/** Inserção recém-criada, como {@code POST /api/atendidos/insercoes} devolve. */
record InsercaoResposta(UUID id, UUID pessoaId, UUID inscricaoId, String nomeCompleto, LocalDate dataInsercao,
		Turno turno, UUID cuidadorResponsavelId, String nomeCuidador, SituacaoInsercao situacao) {
}
