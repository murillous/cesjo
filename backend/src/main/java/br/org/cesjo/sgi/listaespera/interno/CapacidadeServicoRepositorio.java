package br.org.cesjo.sgi.listaespera.interno;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CapacidadeServicoRepositorio extends JpaRepository<CapacidadeServico, UUID> {

	/** A capacidade vigente: a definida por último. */
	Optional<CapacidadeServico> findFirstByOrderByDefinidaEmDescIdDesc();

}
