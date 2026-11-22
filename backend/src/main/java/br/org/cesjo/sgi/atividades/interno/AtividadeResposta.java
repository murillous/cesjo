package br.org.cesjo.sgi.atividades.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.org.cesjo.sgi.atividades.TipoAtividade;
import br.org.cesjo.sgi.institucional.ResumoColaborador;

/** Atividade completa, com a descrição decifrada (ADR 0150). */
record AtividadeResposta(UUID id, TipoAtividade tipo, LocalDate data, String titulo, String descricao,
		List<Participante> participantes, List<ResumoColaborador> profissionais, Instant criadoEm,
		Instant atualizadoEm) {
}
