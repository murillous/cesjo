package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.atendidos.SituacaoInsercao;
import br.org.cesjo.sgi.atendidos.Turno;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/** Uma linha da consulta de leitura da listagem, antes de virar a visão básica ou a completa. */
record LinhaAtendido(UUID id, UUID pessoaId, String nomeCompleto, LocalDate dataNascimento,
		TipoDeficiencia tipoDeficiencia, Turno turno, SituacaoInsercao situacao, LocalDate dataInsercao) {
}
