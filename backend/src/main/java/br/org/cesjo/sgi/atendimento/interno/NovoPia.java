package br.org.cesjo.sgi.atendimento.interno;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Primeira versão do PIA de uma pessoa. */
record NovoPia(@NotNull UUID pessoaId, @NotNull @Valid ConteudoPia conteudo) {
}
