package br.org.cesjo.sgi.atendimento.interno;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EscutaRepositorio extends JpaRepository<Escuta, UUID> {

	List<Escuta> findByPessoaIdOrderByDataEscutaDescCriadoEmDesc(UUID pessoaId);

}
