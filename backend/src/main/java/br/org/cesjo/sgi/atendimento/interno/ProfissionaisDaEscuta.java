package br.org.cesjo.sgi.atendimento.interno;

import java.util.List;
import java.util.UUID;

import br.org.cesjo.sgi.institucional.ResumoColaborador;

/**
 * Colaboradores ativos para escolher o profissional responsável.
 *
 * @param sugeridoId colaborador ligado à conta logada, se houver (ADR 0046)
 */
record ProfissionaisDaEscuta(List<ResumoColaborador> profissionais, UUID sugeridoId) {
}
