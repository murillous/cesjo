package br.org.cesjo.sgi.atividades.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.atividades.DirecaoComunicacao;
import br.org.cesjo.sgi.atividades.TipoComunicacao;

/** Ficha do contato ou comunicação, com a descrição e os dados do anexo ({@code null} sem anexo). */
record ComunicacaoResposta(UUID id, TipoComunicacao tipo, DirecaoComunicacao direcao, LocalDate data,
		String contraparte, String assunto, String descricao, AnexoComunicacao anexo, Instant criadoEm,
		Instant atualizadoEm) {
}
