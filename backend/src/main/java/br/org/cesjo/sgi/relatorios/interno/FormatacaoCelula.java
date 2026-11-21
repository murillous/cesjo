package br.org.cesjo.sgi.relatorios.interno;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Texto das células no PDF e no cabeçalho, no padrão brasileiro. */
final class FormatacaoCelula {

	static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private FormatacaoCelula() {
	}

	static String texto(Object valor) {
		return switch (valor) {
			case null -> "";
			case String texto -> texto;
			case Boolean logico -> logico ? "Sim" : "Não";
			case LocalDate data -> DATA.format(data);
			case LocalDateTime dataHora -> DATA_HORA.format(dataHora);
			case Instant instante -> DATA_HORA.format(instante.atZone(IdentidadeVisual.FUSO));
			case Integer _, Long _, Short _, Byte _, BigInteger _ -> numero(0).format(valor);
			case Number numero -> numero(2).format(numero instanceof BigDecimal decimal ? decimal : numero.doubleValue());
			default -> valor.toString();
		};
	}

	static String dataHora(Instant instante) {
		return DATA_HORA.format(instante.atZone(IdentidadeVisual.FUSO));
	}

	private static NumberFormat numero(int casasMaximas) {
		NumberFormat formato = NumberFormat.getNumberInstance(IdentidadeVisual.IDIOMA);
		formato.setMaximumFractionDigits(casasMaximas);
		// Sem separador de milhar: ano e código (2026) não podem virar "2.026".
		formato.setGroupingUsed(false);
		return formato;
	}

}
