package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Instant;
import java.util.List;

/**
 * Dashboard de perfil dos usuários atendidos (RF017, issue #56, ADR 0195): os usuários com inserção
 * ativa contados por tipo de deficiência, gênero, faixa etária e turno.
 *
 * @param geradoEm quando as contagens foram feitas; o painel é recalculado a cada acesso
 * @param totalAtivos usuários com inserção ativa, a soma de cada gráfico
 */
record PainelPerfil(Instant geradoEm, long totalAtivos, List<Contagem> porTipoDeficiencia, List<Contagem> porGenero,
		List<Contagem> porFaixaEtaria, List<Contagem> porTurno) {
}
