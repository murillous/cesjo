package br.org.cesjo.sgi.atividades.interno;

import br.org.cesjo.sgi.arquivos.MetadadosArquivo;
import br.org.cesjo.sgi.arquivos.TipoArquivo;

/** Dados do anexo mostrados na ficha, sem o conteúdo, que sai só pelo download. */
record AnexoComunicacao(String nomeOriginal, TipoArquivo tipo, long tamanho) {

	static AnexoComunicacao de(MetadadosArquivo metadados) {
		return new AnexoComunicacao(metadados.nomeOriginal(), metadados.tipo(), metadados.tamanho());
	}

}
