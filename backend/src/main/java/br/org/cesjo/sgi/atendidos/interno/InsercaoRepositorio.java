package br.org.cesjo.sgi.atendidos.interno;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.org.cesjo.sgi.atendidos.SituacaoInsercao;

interface InsercaoRepositorio extends JpaRepository<Insercao, UUID> {

	boolean existsByPessoaIdAndSituacao(UUID pessoaId, SituacaoInsercao situacao);

	long countBySituacao(SituacaoInsercao situacao);

	/** Pares turno e quantidade das inserções na situação. */
	@Query("select i.turno, count(i) from Insercao i where i.situacao = :situacao group by i.turno")
	List<Object[]> contarPorTurno(SituacaoInsercao situacao);

}
