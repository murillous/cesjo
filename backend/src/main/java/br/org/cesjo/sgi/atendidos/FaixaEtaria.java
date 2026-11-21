package br.org.cesjo.sgi.atendidos;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

/**
 * Faixa etária do usuário atendido, calculada da data de nascimento (seção 5 do planejamento): jovem de
 * 18 a 29 anos e adulto de 30 a 59. Não é guardada.
 */
public enum FaixaEtaria {

	JOVEM("Jovem (18 a 29 anos)", 18, 29),

	ADULTO("Adulto (30 a 59 anos)", 30, 59);

	private final String rotulo;

	private final int idadeMinima;

	private final int idadeMaxima;

	FaixaEtaria(String rotulo, int idadeMinima, int idadeMaxima) {
		this.rotulo = rotulo;
		this.idadeMinima = idadeMinima;
		this.idadeMaxima = idadeMaxima;
	}

	public String rotulo() {
		return rotulo;
	}

	/** Idade completa em {@code hoje}. */
	public static int idade(LocalDate nascimento, LocalDate hoje) {
		return Period.between(nascimento, hoje).getYears();
	}

	/** Faixa da pessoa, vazia sem data de nascimento ou fora de 18 a 59 anos. */
	public static Optional<FaixaEtaria> de(LocalDate nascimento, LocalDate hoje) {
		if (nascimento == null) {
			return Optional.empty();
		}
		int idade = idade(nascimento, hoje);
		for (FaixaEtaria faixa : values()) {
			if (idade >= faixa.idadeMinima && idade <= faixa.idadeMaxima) {
				return Optional.of(faixa);
			}
		}
		return Optional.empty();
	}

	/** Nascidos depois deste dia são novos demais para a faixa. */
	public LocalDate nascidoAte(LocalDate hoje) {
		return hoje.minusYears(idadeMinima);
	}

	/** Nascidos neste dia ou antes são velhos demais para a faixa. */
	public LocalDate nascidoDepoisDe(LocalDate hoje) {
		return hoje.minusYears(idadeMaxima + 1L);
	}

}
