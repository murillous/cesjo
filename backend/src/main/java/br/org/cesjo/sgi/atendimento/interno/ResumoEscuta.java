package br.org.cesjo.sgi.atendimento.interno;

import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.institucional.ResumoColaborador;

/** Escuta na lista de uma pessoa: data e profissional, sem os textos sensíveis. */
record ResumoEscuta(UUID id, UUID pessoaId, LocalDate dataEscuta, ResumoColaborador profissional) {
}
