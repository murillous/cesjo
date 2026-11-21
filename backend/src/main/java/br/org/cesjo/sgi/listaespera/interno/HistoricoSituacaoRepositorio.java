package br.org.cesjo.sgi.listaespera.interno;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface HistoricoSituacaoRepositorio extends JpaRepository<HistoricoSituacao, UUID> {

	List<HistoricoSituacao> findByInscricaoIdOrderByRegistradoEmAscIdAsc(UUID inscricaoId);

}
