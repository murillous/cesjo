package br.org.cesjo.sgi.institucional.interno;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ArticulacaoRepositorio extends JpaRepository<ArticulacaoParceria, UUID> {

	List<ArticulacaoParceria> findByParceiroIdOrderByDataDescCriadoEmDesc(UUID parceiroId);

}
