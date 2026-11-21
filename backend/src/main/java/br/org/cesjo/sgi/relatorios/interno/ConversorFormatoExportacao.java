package br.org.cesjo.sgi.relatorios.interno;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.relatorios.FormatoExportacao;

/**
 * Deixa os controladores receberem {@code @RequestParam FormatoExportacao formato} com {@code pdf} ou
 * {@code xlsx} na URL (ADR 0042). O Spring Boot registra todo {@link Converter} do contexto no MVC; um
 * valor desconhecido vira 400 pelo tratamento padrão de parâmetro inválido.
 */
@Component
class ConversorFormatoExportacao implements Converter<String, FormatoExportacao> {

	@Override
	public FormatoExportacao convert(String texto) {
		return FormatoExportacao.doParametro(texto);
	}

}
