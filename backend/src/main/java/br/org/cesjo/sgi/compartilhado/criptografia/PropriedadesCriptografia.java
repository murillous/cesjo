package br.org.cesjo.sgi.compartilhado.criptografia;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chaves da criptografia de campos e arquivos (ADR 0004 e ADR 0025).
 *
 * <p>
 * Em produção vêm de variáveis de ambiente: {@code SGI_CRIPTOGRAFIA_CHAVEATIVA} e uma
 * {@code SGI_CRIPTOGRAFIA_CHAVES_<ID>} para cada chave, em Base64. A validação fica em
 * {@link Cifrador}, que impede a aplicação de subir sem chave ativa válida.
 *
 * @param chaveAtiva identificador da chave usada para cifrar valores novos
 * @param chaves chaves de 32 bytes em Base64, por identificador; as antigas ficam aqui para decifrar
 * valores ainda não recifrados
 */
@ConfigurationProperties("sgi.criptografia")
public record PropriedadesCriptografia(String chaveAtiva, Map<String, String> chaves) {

	public PropriedadesCriptografia {
		chaves = chaves == null ? Map.of() : Map.copyOf(chaves);
	}

}
