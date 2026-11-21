package br.org.cesjo.sgi.listaespera.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.DadosPessoa;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/** Inscrição na lista de espera com os dados da pessoa, como a API devolve. */
record InscricaoResposta(UUID id, UUID pessoaId, String nomeCompleto, LocalDate dataNascimento,
		TipoDeficiencia tipoDeficiencia, String telefone, String nomeResponsavel,
		OrigemEncaminhamento origemEncaminhamento, LocalDate dataCadastro, SituacaoInscricao situacao,
		Instant criadoEm) {

	static InscricaoResposta de(InscricaoListaEspera inscricao, DadosPessoa pessoa) {
		return new InscricaoResposta(inscricao.getId(), pessoa.id(), pessoa.nomeCompleto(), pessoa.dataNascimento(),
				pessoa.tipoDeficiencia(), pessoa.telefone(), pessoa.nomeResponsavel(),
				inscricao.getOrigemEncaminhamento(), inscricao.getDataCadastro(), inscricao.getSituacao(),
				inscricao.getCriadoEm());
	}

}
