package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface AtividadeRepositorio extends JpaRepository<Atividade, UUID>, JpaSpecificationExecutor<Atividade> {

	/** Pares tipo e quantidade das atividades com data no período, as duas pontas inclusive. */
	@Query("select a.tipo, count(a) from Atividade a where a.data between :inicio and :fim group by a.tipo")
	List<Object[]> contarPorTipo(LocalDate inicio, LocalDate fim);

	@Query("select distinct p from Atividade a join a.profissionais p")
	List<UUID> profissionaisComAtividade();

	@Query("select distinct p from Atividade a join a.participantes p")
	List<UUID> participantesComAtividade();

}
