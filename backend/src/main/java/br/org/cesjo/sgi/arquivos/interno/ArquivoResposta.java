package br.org.cesjo.sgi.arquivos.interno;

import java.time.Instant;
import java.util.UUID;

import br.org.cesjo.sgi.arquivos.MetadadosArquivo;

/** Metadados devolvidos pela API depois do envio. */
record ArquivoResposta(UUID id, String nome, String tipoMime, long tamanho, String sha256, Instant enviadoEm) {

	static ArquivoResposta de(MetadadosArquivo metadados) {
		return new ArquivoResposta(metadados.id(), metadados.nomeOriginal(), metadados.tipo().tipoMime(),
				metadados.tamanho(), metadados.sha256(), metadados.enviadoEm());
	}

}
