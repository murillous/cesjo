package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface ComunicacaoRepositorio extends JpaRepository<Comunicacao, UUID>, JpaSpecificationExecutor<Comunicacao> {

	/** Pares tipo e quantidade das comunicações com data no período, as duas pontas inclusive. */
	@Query("select c.tipo, count(c) from Comunicacao c where c.data between :inicio and :fim group by c.tipo")
	List<Object[]> contarPorTipo(LocalDate inicio, LocalDate fim);

}
