package br.org.cesjo.sgi.institucional.interno;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.institucional.DirecaoDoacao;
import br.org.cesjo.sgi.institucional.TipoDoacao;

/**
 * Dados do registro e da edição de doação (RF015, ADR 0178).
 *
 * <p>
 * PENDENTE-CESJO(P11): o valor em reais é opcional e só existe na doação em dinheiro; não há recibo nem
 * prestação de contas (ADR 0178).
 *
 * @param contraparte quem doou ao Centro-Dia ou recebeu dele
 * @param quantidade quantidade em texto livre, por exemplo "20 kg" ou "3 caixas"
 */
record DadosDoacao(@NotNull DirecaoDoacao direcao,
		@NotNull @PastOrPresent(message = "a data não pode ser futura") LocalDate data,
		@NotBlank @Size(max = 200) String contraparte, @NotNull TipoDoacao tipo,
		@NotBlank @Size(max = 2000) String descricao, @Size(max = 100) String quantidade,
		@DecimalMin(value = "0.01", message = "o valor precisa ser maior que zero")
		@DecimalMax(value = "9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal valor) {

	@AssertTrue(message = "só doação em dinheiro tem valor")
	boolean isValorSoEmDinheiro() {
		return valor == null || tipo == TipoDoacao.DINHEIRO;
	}

	BigDecimal valorNormalizado() {
		return valor == null ? null : valor.setScale(2, RoundingMode.UNNECESSARY);
	}

	/** Texto sem espaços nas pontas, ou nulo quando vazio. */
	static String texto(String valor) {
		return valor == null || valor.isBlank() ? null : valor.strip();
	}

}
