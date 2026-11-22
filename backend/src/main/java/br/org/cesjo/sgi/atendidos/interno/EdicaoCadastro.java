package br.org.cesjo.sgi.atendidos.interno;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.pessoas.CadastroComplementar;
import br.org.cesjo.sgi.pessoas.Cpf;
import br.org.cesjo.sgi.pessoas.CrencaReligiosa;
import br.org.cesjo.sgi.pessoas.DadosSensiveis;
import br.org.cesjo.sgi.pessoas.Escolaridade;
import br.org.cesjo.sgi.pessoas.Genero;
import br.org.cesjo.sgi.pessoas.Telefone;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Edição do cadastro completo do usuário atendido (RF007, ADR 0140): os dados da pessoa que vêm da
 * lista de espera, os complementares e, para quem pode, a crença e a saúde.
 *
 * @param dataNascimento opcional; a faixa etária sai dela
 * @param dadosSensiveis nulo deixa a crença e a saúde como estão; preenchido exige
 * {@code ATENDIDOS_DADOS_SENSIVEIS} (ADR 0142)
 */
record EdicaoCadastro(@NotBlank @Size(max = 150) String nomeCompleto,
		// PENDENTE-CESJO(P2): data de nascimento opcional e sem bloqueio fora de 18 a 59 anos (ADR 0038).
		@Past(message = "a data de nascimento precisa ser anterior a hoje") LocalDate dataNascimento,
		@NotNull TipoDeficiencia tipoDeficiencia,
		@NotBlank @Pattern(regexp = Telefone.FORMATO, message = Telefone.MENSAGEM) String telefone,
		@NotNull @Valid Complementar cadastro, @Valid Sensiveis dadosSensiveis) {

	/** Campos de {@link CadastroComplementar}, com as regras de formato (ADR 0140). */
	record Complementar(Genero genero, @Size(max = 14) String cpf,
			@Pattern(regexp = "^[\\d.\\s-]*$", message = "use só números") @Size(max = 14) String nis,
			@Size(max = 20) String rg, Escolaridade escolaridade,
			@Pattern(regexp = "^(\\d{5}-?\\d{3})?$", message = "informe os 8 dígitos do CEP") String cep,
			@Size(max = 150) String logradouro, @Size(max = 20) String numero, @Size(max = 80) String complemento,
			@Size(max = 80) String bairro, @Size(max = 80) String cidade,
			@DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal rendaFamiliar, boolean recebeBpc,
			boolean inscritoCadUnico, boolean recebeBolsaFamilia) {

		@AssertTrue(message = Cpf.MENSAGEM)
		boolean isCpfValido() {
			return Cpf.valido(cpf);
		}

		@AssertTrue(message = "o NIS tem 11 dígitos")
		boolean isNisValido() {
			return nis == null || nis.isBlank() || nis.replaceAll("\\D", "").length() == 11;
		}

		CadastroComplementar paraPessoas() {
			return new CadastroComplementar(genero, cpf, nis, rg, escolaridade, cep, logradouro, numero, complemento,
					bairro, cidade, rendaFamiliar, recebeBpc, inscritoCadUnico, recebeBolsaFamilia);
		}

	}

	/** Campos de {@link DadosSensiveis}. */
	record Sensiveis(CrencaReligiosa crencaReligiosa, @Size(max = 2000) String medicamentosUsoContinuo,
			@Size(max = 2000) String observacoesSaude) {

		DadosSensiveis paraPessoas() {
			return new DadosSensiveis(crencaReligiosa, medicamentosUsoContinuo, observacoesSaude);
		}

	}

}
