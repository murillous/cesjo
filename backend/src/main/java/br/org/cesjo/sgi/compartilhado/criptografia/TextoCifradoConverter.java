package br.org.cesjo.sgi.compartilhado.criptografia;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Cifra um campo de texto da entidade ao gravar e decifra ao ler (ADR 0004 e ADR 0025). O banco guarda
 * o formato textual {@code v1:<id>:<Base64>}; a coluna deve ser {@code text}.
 *
 * <p>
 * Uso: {@code @Convert(converter = TextoCifradoConverter.class)} no campo. O Hibernate cria o conversor
 * pelo Spring, que injeta o {@link Cifrador}. Campo cifrado não é pesquisável nem ordenável.
 */
@Converter
public class TextoCifradoConverter implements AttributeConverter<String, String> {

	private final Cifrador cifrador;

	public TextoCifradoConverter(Cifrador cifrador) {
		this.cifrador = cifrador;
	}

	@Override
	public String convertToDatabaseColumn(String atributo) {
		return cifrador.cifrarTexto(atributo);
	}

	@Override
	public String convertToEntityAttribute(String coluna) {
		return cifrador.decifrarTexto(coluna);
	}

}
