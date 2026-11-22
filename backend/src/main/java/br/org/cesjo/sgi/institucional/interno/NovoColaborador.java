package br.org.cesjo.sgi.institucional.interno;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.institucional.FuncaoColaborador;

/**
 * Dados do cadastro de colaborador (ADR 0045). O colaborador novo nasce ativo.
 *
 * @param contaId conta de acesso a ligar, opcional (ADR 0046)
 * @param dataContratacao quando informada, já abre o primeiro vínculo (ADR 0170)
 */
record NovoColaborador(@NotBlank @Size(max = 150) String nome, @NotNull FuncaoColaborador funcao, UUID contaId,
		@PastOrPresent(message = "a data de contratação não pode ser futura") LocalDate dataContratacao) {
}
