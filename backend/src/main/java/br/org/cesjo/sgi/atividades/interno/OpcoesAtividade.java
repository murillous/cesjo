package br.org.cesjo.sgi.atividades.interno;

import java.util.List;
import java.util.UUID;

import br.org.cesjo.sgi.atendidos.UsuarioAtivo;
import br.org.cesjo.sgi.institucional.ResumoColaborador;

/**
 * O que pode ser escolhido no registro da atividade.
 *
 * @param sugeridoId colaborador ligado à conta logada, se houver (ADR 0046)
 */
record OpcoesAtividade(List<UsuarioAtivo> usuarios, List<ResumoColaborador> profissionais, UUID sugeridoId) {
}
