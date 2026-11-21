package br.org.cesjo.sgi.atendimento.interno;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Registro de escuta para uma pessoa. */
record NovaEscuta(@NotNull UUID pessoaId, @NotNull @Valid DadosEscuta dados) {
}
