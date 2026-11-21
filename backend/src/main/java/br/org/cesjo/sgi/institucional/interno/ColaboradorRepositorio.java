package br.org.cesjo.sgi.institucional.interno;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ColaboradorRepositorio extends JpaRepository<Colaborador, UUID> {

	Page<Colaborador> findByAtivo(boolean ativo, Pageable paginacao);

	List<Colaborador> findByAtivoTrueOrderByNomeAscIdAsc();

	Optional<Colaborador> findByIdAndAtivoTrue(UUID id);

	long countByIdInAndAtivoTrue(Collection<UUID> ids);

	Optional<Colaborador> findByContaAcessoIdAndAtivoTrue(UUID contaAcessoId);

	Optional<Colaborador> findByContaAcessoId(UUID contaAcessoId);

}
