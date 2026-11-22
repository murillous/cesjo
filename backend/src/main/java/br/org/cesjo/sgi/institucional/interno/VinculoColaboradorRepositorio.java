package br.org.cesjo.sgi.institucional.interno;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface VinculoColaboradorRepositorio extends JpaRepository<VinculoColaborador, UUID> {

	/** Histórico do colaborador, do vínculo mais recente ao mais antigo. */
	List<VinculoColaborador> findByColaboradorIdOrderByDataContratacaoDescCriadoEmDesc(UUID colaboradorId);

	Optional<VinculoColaborador> findByColaboradorIdAndDataDesligamentoIsNull(UUID colaboradorId);

	boolean existsByColaboradorId(UUID colaboradorId);

}
