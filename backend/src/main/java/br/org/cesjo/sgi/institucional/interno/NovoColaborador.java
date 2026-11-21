package br.org.cesjo.sgi.institucional.interno;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.institucional.FuncaoColaborador;

/**
 * Dados do cadastro de colaborador (ADR 0045). O colaborador novo nasce ativo.
 *
 * @param contaId conta de acesso a ligar, opcional (ADR 0046)
 */
record NovoColaborador(@NotBlank @Size(max = 150) String nome, @NotNull FuncaoColaborador funcao, UUID contaId) {
}
