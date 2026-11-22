package br.org.cesjo.sgi.atividades.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.atividades.TipoComunicacao;

/** Ficha do contato ou comunicação, com a descrição. */
record ComunicacaoResposta(UUID id, TipoComunicacao tipo, LocalDate data, String contraparte, String assunto,
		String descricao, Instant criadoEm, Instant atualizadoEm) {
}
