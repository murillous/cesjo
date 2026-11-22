package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.atividades.TipoComunicacao;

/**
 * Campos do contato ou comunicação (RF011, ADR 0160), no registro e na correção.
 *
 * @param contraparte pessoa ou instituição do outro lado: o cuidador familiar, a instituição contatada ou
 * a pessoa atendida
 * @param descricao detalhes, opcionais; guardados cifrados
 */
// PENDENTE-CESJO(P30): a pessoa ou instituição é texto livre, sem ligar o contato a um usuário atendido
// nem a um cadastro de instituições (ADR 0160).
record DadosComunicacao(@NotNull TipoComunicacao tipo,
		@NotNull @PastOrPresent(message = "a data não pode ser futura") LocalDate data,
		@NotBlank @Size(max = 200) String contraparte, @NotBlank @Size(max = 200) String assunto,
		@Size(max = 10_000) String descricao) {

	/** Descrição sem espaços nas pontas, ou {@code null} quando vazia. */
	String descricaoLimpa() {
		return descricao == null || descricao.isBlank() ? null : descricao.strip();
	}

}
