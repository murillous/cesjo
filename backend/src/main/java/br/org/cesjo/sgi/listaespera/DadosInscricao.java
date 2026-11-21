package br.org.cesjo.sgi.listaespera;

import java.time.LocalDate;
import java.util.UUID;

/** Inscrição na lista de espera, como o módulo devolve para os outros módulos (ADR 0081). */
public record DadosInscricao(UUID id, UUID pessoaId, LocalDate dataCadastro, OrigemEncaminhamento origemEncaminhamento,
		SituacaoInscricao situacao) {
}
