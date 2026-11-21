package br.org.cesjo.sgi.listaespera.interno;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.org.cesjo.sgi.listaespera.SituacaoInscricao;

interface InscricaoRepositorio
		extends JpaRepository<InscricaoListaEspera, UUID>, JpaSpecificationExecutor<InscricaoListaEspera> {

	List<InscricaoListaEspera> findBySituacaoIn(Collection<SituacaoInscricao> situacoes, Sort ordem);

	long countBySituacao(SituacaoInscricao situacao);

}
