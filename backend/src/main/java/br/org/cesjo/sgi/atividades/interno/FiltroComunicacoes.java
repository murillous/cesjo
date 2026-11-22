package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import br.org.cesjo.sgi.atividades.TipoComunicacao;

/**
 * Filtros da listagem de contatos e comunicações. Todos opcionais; o período inclui as duas pontas.
 */
record FiltroComunicacoes(TipoComunicacao tipo, LocalDate inicio, LocalDate fim) {

	Specification<Comunicacao> especificacao() {
		return (raiz, consulta, cb) -> {
			List<Predicate> condicoes = new ArrayList<>();
			if (tipo != null) {
				condicoes.add(cb.equal(raiz.get("tipo"), tipo));
			}
			if (inicio != null) {
				condicoes.add(cb.greaterThanOrEqualTo(raiz.get("data"), inicio));
			}
			if (fim != null) {
				condicoes.add(cb.lessThanOrEqualTo(raiz.get("data"), fim));
			}
			return cb.and(condicoes.toArray(Predicate[]::new));
		};
	}

}
