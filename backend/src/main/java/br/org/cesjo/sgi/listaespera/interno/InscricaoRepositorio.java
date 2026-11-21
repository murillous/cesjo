package br.org.cesjo.sgi.listaespera.interno;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.org.cesjo.sgi.listaespera.SituacaoInscricao;

interface InscricaoRepositorio
		extends JpaRepository<InscricaoListaEspera, UUID>, JpaSpecificationExecutor<InscricaoListaEspera> {

	List<InscricaoListaEspera> findBySituacaoIn(Collection<SituacaoInscricao> situacoes, Sort ordem);

	/** Inscrições nas situações dadas, na ordem da fila (índice {@code ix_inscricao_fila}). */
	List<InscricaoListaEspera> findBySituacaoInOrderByDataCadastroAscCriadoEmAsc(Collection<SituacaoInscricao> situacoes);

	Optional<InscricaoListaEspera> findFirstByPessoaIdAndSituacaoInOrderByDataCadastroAsc(UUID pessoaId,
			Collection<SituacaoInscricao> situacoes);

}
