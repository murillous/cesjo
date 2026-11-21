package br.org.cesjo.sgi.institucional.interno;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.institucional.FuncaoColaborador;

/**
 * Edição completa do colaborador (ADR 0045). {@code contaId} nulo desfaz a ligação com a conta.
 */
record EdicaoColaborador(@NotBlank @Size(max = 150) String nome, @NotNull FuncaoColaborador funcao,
		@NotNull Boolean ativo, UUID contaId) {
}
