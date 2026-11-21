package br.org.cesjo.sgi.listaespera.interno;

import java.time.Instant;

import br.org.cesjo.sgi.pessoas.DocumentoPessoa;
import br.org.cesjo.sgi.pessoas.TipoDocumento;

/** Documento anexado à inscrição, sem o conteúdo, como a API devolve. */
record DocumentoResposta(TipoDocumento tipo, String nomeArquivo, String tipoMime, long tamanho,
		Instant anexadoEm) {

	static DocumentoResposta de(DocumentoPessoa documento) {
		return new DocumentoResposta(documento.tipo(), documento.nomeArquivo(), documento.tipoMime(),
				documento.tamanho(), documento.anexadoEm());
	}

}
