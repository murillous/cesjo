package br.org.cesjo.sgi.institucional.interno;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.org.cesjo.sgi.institucional.DirecaoDoacao;
import br.org.cesjo.sgi.institucional.TipoDoacao;

interface DoacaoRepositorio extends JpaRepository<Doacao, UUID>, JpaSpecificationExecutor<Doacao> {

	/** Filtros opcionais: período (datas inclusivas), direção e tipo. Nulo não filtra. */
	static Specification<Doacao> filtro(LocalDate de, LocalDate ate, DirecaoDoacao direcao, TipoDoacao tipo) {
		return (raiz, consulta, cb) -> {
			List<Predicate> condicoes = new ArrayList<>();
			if (de != null) {
				condicoes.add(cb.greaterThanOrEqualTo(raiz.get("data"), de));
			}
			if (ate != null) {
				condicoes.add(cb.lessThanOrEqualTo(raiz.get("data"), ate));
			}
			if (direcao != null) {
				condicoes.add(cb.equal(raiz.get("direcao"), direcao));
			}
			if (tipo != null) {
				condicoes.add(cb.equal(raiz.get("tipo"), tipo));
			}
			return cb.and(condicoes.toArray(Predicate[]::new));
		};
	}

}
