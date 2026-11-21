package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * O que a tela de inserção precisa saber do candidato antes de inserir: quem é, se ainda aguarda na
 * fila e quais responsáveis podem ser o cuidador.
 */
record CandidatoInsercao(UUID inscricaoId, UUID pessoaId, String nomeCompleto, TipoDeficiencia tipoDeficiencia,
		LocalDate dataCadastro, SituacaoInscricao situacao, List<Responsavel> responsaveis) {

	record Responsavel(UUID id, String nomeCompleto, boolean principal) {
	}

}
