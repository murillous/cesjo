package br.org.cesjo.sgi.institucional.interno;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.org.cesjo.sgi.institucional.TipoEventoInstitucional;

interface EventoRepositorio
		extends JpaRepository<EventoInstitucional, UUID>, JpaSpecificationExecutor<EventoInstitucional> {

	/** Filtros opcionais: tipo, período (datas inclusivas) e parceiro. Nulo não filtra. */
	static Specification<EventoInstitucional> filtro(TipoEventoInstitucional tipo, LocalDate de, LocalDate ate,
			UUID parceiroId) {
		return (raiz, consulta, cb) -> {
			List<Predicate> condicoes = new ArrayList<>();
			if (tipo != null) {
				condicoes.add(cb.equal(raiz.get("tipo"), tipo));
			}
			if (de != null) {
				condicoes.add(cb.greaterThanOrEqualTo(raiz.get("data"), de));
			}
			if (ate != null) {
				condicoes.add(cb.lessThanOrEqualTo(raiz.get("data"), ate));
			}
			if (parceiroId != null) {
				condicoes.add(cb.equal(raiz.get("parceiroId"), parceiroId));
			}
			return cb.and(condicoes.toArray(Predicate[]::new));
		};
	}

}
