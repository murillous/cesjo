package br.org.cesjo.sgi.institucional.interno;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ParceiroRepositorio extends JpaRepository<Parceiro, UUID> {

	/** Filtros opcionais: situação e parte do nome, sem diferenciar maiúsculas. */
	@Query("""
			select p from Parceiro p
			where (:ativo is null or p.ativo = :ativo)
			  and (:nome is null or lower(p.nome) like lower(concat('%', cast(:nome as string), '%')))""")
	Page<Parceiro> buscar(@Param("ativo") Boolean ativo, @Param("nome") String nome, Pageable paginacao);

}
