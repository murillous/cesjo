package br.org.cesjo.sgi.atendimento.interno;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface VisitaRepositorio extends JpaRepository<VisitaDomiciliar, UUID> {

	/** Visitas com data e hora em {@code [inicio, fim)}, em ordem de horário (índice {@code ix_visita_data_hora}). */
	List<VisitaDomiciliar> findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAscIdAsc(
			LocalDateTime inicio, LocalDateTime fim);

}
