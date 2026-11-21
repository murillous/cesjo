package br.org.cesjo.sgi.atendimento.interno;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PiaRepositorio extends JpaRepository<Pia, UUID> {

	/** Versões do PIA da pessoa, da mais nova para a mais antiga. */
	List<Pia> findByPessoaIdOrderByNumeroVersaoDesc(UUID pessoaId);

	Optional<Pia> findFirstByPessoaIdOrderByNumeroVersaoDesc(UUID pessoaId);

}
