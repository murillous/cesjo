package br.org.cesjo.sgi.atendidos.interno;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DesligamentoRepositorio extends JpaRepository<Desligamento, UUID> {

	Optional<Desligamento> findByInsercaoId(UUID insercaoId);

}
