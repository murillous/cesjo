package br.org.cesjo.sgi.relatorios;

import java.util.Objects;

/**
 * Um filtro usado na consulta exportada, impresso no cabeçalho do PDF e da planilha para quem lê saber
 * o recorte dos dados (ADR 0041).
 *
 * @param nome rótulo do filtro, por exemplo {@code Situação}
 * @param valor valor escolhido, já no texto que a pessoa vê, por exemplo {@code Aguardando}
 */
public record FiltroAplicado(String nome, String valor) {

	public FiltroAplicado {
		Objects.requireNonNull(nome, "nome");
		Objects.requireNonNull(valor, "valor");
	}

}
