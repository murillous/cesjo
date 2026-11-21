package br.org.cesjo.sgi.pessoas.interno;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ResponsavelRepositorio extends JpaRepository<Responsavel, UUID> {

	/** O primeiro responsável cadastrado, que é o do RF001. */
	Optional<Responsavel> findFirstByPessoaIdOrderByCriadoEmAsc(UUID pessoaId);

}
