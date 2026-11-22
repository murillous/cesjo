package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.atividades.DirecaoComunicacao;
import br.org.cesjo.sgi.atividades.TipoComunicacao;

/**
 * Campos do contato ou comunicação (RF011, ADR 0160 e ADR 0164), no registro e na correção. O anexo
 * tem rota própria.
 *
 * @param direcao enviada ou recebida; só nos ofícios, declarações e encaminhamentos
 * @param contraparte pessoa ou instituição do outro lado: o cuidador familiar, a instituição contatada, a
 * pessoa atendida ou a instituição que expediu ou recebeu o documento
 * @param descricao detalhes, opcionais; guardados cifrados
 */
// PENDENTE-CESJO(P30): a pessoa ou instituição é texto livre, sem ligar o contato a um usuário atendido
// nem a um cadastro de instituições (ADR 0160).
record DadosComunicacao(@NotNull TipoComunicacao tipo, DirecaoComunicacao direcao,
		@NotNull @PastOrPresent(message = "a data não pode ser futura") LocalDate data,
		@NotBlank @Size(max = 200) String contraparte, @NotBlank @Size(max = 200) String assunto,
		@Size(max = 10_000) String descricao) {

	@AssertTrue(message = "informe se foi enviado ou recebido, só para ofício, declaração e encaminhamento")
	boolean isDirecaoDoTipo() {
		return tipo == null || tipo.temDirecao() == (direcao != null);
	}

	/** Descrição sem espaços nas pontas, ou {@code null} quando vazia. */
	String descricaoLimpa() {
		return descricao == null || descricao.isBlank() ? null : descricao.strip();
	}

}
