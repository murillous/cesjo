package br.org.cesjo.sgi.atividades.interno;

import java.util.List;

import br.org.cesjo.sgi.institucional.ResumoColaborador;

/**
 * Quem pode ser escolhido nos filtros da listagem: profissionais e usuários que aparecem em alguma
 * atividade, ativos ou não, em ordem de nome.
 */
record OpcoesFiltro(List<ResumoColaborador> profissionais, List<Participante> usuarios) {
}
