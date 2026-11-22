package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.atividades.DirecaoComunicacao;
import br.org.cesjo.sgi.atividades.TipoComunicacao;

/** Contato ou comunicação na listagem: sem a descrição, que só sai na ficha. */
record ResumoComunicacao(UUID id, TipoComunicacao tipo, DirecaoComunicacao direcao, LocalDate data,
		String contraparte, String assunto, boolean temAnexo) {
}
