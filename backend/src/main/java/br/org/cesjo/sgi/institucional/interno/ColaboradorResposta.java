package br.org.cesjo.sgi.institucional.interno;

import java.time.Instant;
import java.util.UUID;

import br.org.cesjo.sgi.acesso.ResumoConta;
import br.org.cesjo.sgi.institucional.FuncaoColaborador;

/**
 * Colaborador como a API devolve.
 *
 * @param conta conta de acesso ligada, ou nulo
 */
record ColaboradorResposta(UUID id, String nome, FuncaoColaborador funcao, boolean ativo, ResumoConta conta,
		Instant criadoEm, Instant atualizadoEm) {

	static ColaboradorResposta de(Colaborador colaborador, ResumoConta conta) {
		return new ColaboradorResposta(colaborador.getId(), colaborador.getNome(), colaborador.getFuncao(),
				colaborador.isAtivo(), conta, colaborador.getCriadoEm(), colaborador.getAtualizadoEm());
	}

}
