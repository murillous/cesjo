package br.org.cesjo.sgi.importacao.interno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Uma linha de dados lida da planilha, com os valores por coluna e os erros encontrados na conversão
 * (ADR 0215). Os métodos de leitura acumulam os erros em vez de parar no primeiro, para a pré-visualização
 * mostrar tudo o que precisa ser corrigido na linha.
 */
final class LinhaPlanilha {

	/** Formato das datas digitadas como texto: dd/mm/aaaa, com ou sem zero à esquerda. */
	static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

	private static final DateTimeFormatter DATA_EXIBIDA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

	private final int numero;

	private final Map<Coluna, Object> valores;

	private final List<String> erros = new ArrayList<>();

	/**
	 * @param numero número da linha como aparece no Excel, contando de 1
	 * @param valores valor de cada coluna: {@code String} já sem espaços nas pontas, {@code LocalDate} para
	 * célula de data, ou ausente quando a célula está vazia
	 */
	LinhaPlanilha(int numero, Map<Coluna, Object> valores) {
		this.numero = numero;
		this.valores = valores;
	}

	int numero() {
		return numero;
	}

	List<String> erros() {
		return Collections.unmodifiableList(erros);
	}

	boolean valida() {
		return erros.isEmpty();
	}

	void erro(String mensagem) {
		erros.add(mensagem);
	}

	/** Texto da coluna, ou {@code null} quando vazia. Registra erro se for obrigatória e vazia ou longa demais. */
	String texto(Coluna coluna, int tamanhoMaximo) {
		String texto = exibido(coluna);
		if (texto.isEmpty()) {
			exigir(coluna);
			return null;
		}
		if (texto.length() > tamanhoMaximo) {
			erro(coluna.titulo() + ": use no máximo " + tamanhoMaximo + " caracteres.");
		}
		return texto;
	}

	/** Data da coluna, de célula de data do Excel ou de texto dd/mm/aaaa, ou {@code null} quando vazia. */
	LocalDate data(Coluna coluna) {
		Object valor = valores.get(coluna);
		if (valor == null) {
			exigir(coluna);
			return null;
		}
		if (valor instanceof LocalDate data) {
			return data;
		}
		try {
			return LocalDate.parse(valor.toString(), DATA);
		}
		catch (DateTimeParseException erro) {
			erro(coluna.titulo() + ": use uma data válida no formato dd/mm/aaaa.");
			return null;
		}
	}

	/** Valor de uma coluna de opções, ou {@code null} quando vazia ou fora dos valores aceitos. */
	<E extends Enum<E>> E opcao(Coluna coluna, Opcoes<E> opcoes) {
		String texto = exibido(coluna);
		if (texto.isEmpty()) {
			exigir(coluna);
			return null;
		}
		return opcoes.achar(texto).orElseGet(() -> {
			erro(coluna.titulo() + ": \"" + texto + "\" não é um valor aceito. Use " + String.join(", ", opcoes.rotulos())
					+ ".");
			return null;
		});
	}

	/** Valor da coluna como aparece na pré-visualização: datas em dd/mm/aaaa, vazio quando não há. */
	String exibido(Coluna coluna) {
		Object valor = valores.get(coluna);
		if (valor == null) {
			return "";
		}
		return valor instanceof LocalDate data ? data.format(DATA_EXIBIDA) : valor.toString();
	}

	private void exigir(Coluna coluna) {
		if (coluna.obrigatoria()) {
			erro("Preencha a coluna " + coluna.titulo() + ".");
		}
	}

}
