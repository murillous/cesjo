package br.org.cesjo.sgi.atendimento.interno;

import java.util.UUID;

/** Pessoa como as telas de escuta e PIA a mostram: só o nome (ADR 0102). */
record PessoaAtendida(UUID id, String nomeCompleto) {
}
