package br.org.cesjo.sgi.listaespera.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.pessoas.NovaPessoa;
import br.org.cesjo.sgi.pessoas.Telefone;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Edição dos dados do candidato (ADR 0090): as mesmas regras do cadastro (ADR 0038), sem os anexos,
 * que têm endpoint próprio (ADR 0056).
 */
record EdicaoInscricao(@NotBlank @Size(max = 150) String nomeCompleto,
		// PENDENTE-CESJO(P2): data de nascimento opcional e sem bloqueio fora de 18 a 59 anos (ADR 0038).
		@Past(message = "a data de nascimento precisa ser anterior a hoje") LocalDate dataNascimento,
		@NotNull TipoDeficiencia tipoDeficiencia,
		@NotBlank @Pattern(regexp = Telefone.FORMATO, message = Telefone.MENSAGEM) String telefone,
		@NotBlank @Size(max = 150) String nomeResponsavel, @NotNull OrigemEncaminhamento origemEncaminhamento,
		@NotNull @PastOrPresent(message = "a data de cadastro não pode ser futura") LocalDate dataCadastro) {

	NovaPessoa pessoa() {
		return new NovaPessoa(nomeCompleto, dataNascimento, tipoDeficiencia, telefone, nomeResponsavel);
	}

}
