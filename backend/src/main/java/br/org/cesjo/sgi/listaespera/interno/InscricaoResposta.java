package br.org.cesjo.sgi.listaespera.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.DadosPessoa;
import br.org.cesjo.sgi.pessoas.DocumentoPessoa;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;
import br.org.cesjo.sgi.pessoas.TipoDocumento;

/**
 * Inscrição na lista de espera com os dados da pessoa, como a API devolve.
 *
 * @param documentos documentos anexados, sem o conteúdo
 * @param documentosPendentes documentos obrigatórios que faltam (ADR 0056)
 * @param cadastroCompleto {@code true} quando não falta nenhum documento obrigatório
 */
record InscricaoResposta(UUID id, UUID pessoaId, String nomeCompleto, LocalDate dataNascimento,
		TipoDeficiencia tipoDeficiencia, String telefone, String nomeResponsavel,
		OrigemEncaminhamento origemEncaminhamento, LocalDate dataCadastro, SituacaoInscricao situacao,
		Instant criadoEm, List<DocumentoResposta> documentos, List<TipoDocumento> documentosPendentes,
		boolean cadastroCompleto) {

	static InscricaoResposta de(InscricaoListaEspera inscricao, DadosPessoa pessoa, List<DocumentoPessoa> documentos) {
		List<TipoDocumento> pendentes = DocumentosObrigatorios
			.pendentes(documentos.stream().map(DocumentoPessoa::tipo).toList());
		return new InscricaoResposta(inscricao.getId(), pessoa.id(), pessoa.nomeCompleto(), pessoa.dataNascimento(),
				pessoa.tipoDeficiencia(), pessoa.telefone(), pessoa.nomeResponsavel(),
				inscricao.getOrigemEncaminhamento(), inscricao.getDataCadastro(), inscricao.getSituacao(),
				inscricao.getCriadoEm(), documentos.stream().map(DocumentoResposta::de).toList(), pendentes,
				pendentes.isEmpty());
	}

}
