package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.atendidos.MotivoDesligamento;
import br.org.cesjo.sgi.atendidos.SituacaoInsercao;

/** Desligamento registrado. A observação não volta na resposta: quem quiser lê na ficha, auditado. */
record DesligamentoResposta(UUID id, UUID insercaoId, LocalDate data, MotivoDesligamento motivo,
		SituacaoInsercao situacao) {
}
