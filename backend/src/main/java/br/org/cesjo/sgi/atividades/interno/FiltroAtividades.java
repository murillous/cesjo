package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import br.org.cesjo.sgi.atividades.TipoAtividade;

/**
 * Filtros da listagem de atividades (issue #47, ADR 0155). Todos opcionais; os informados se somam.
 *
 * @param de primeiro dia do período, inclusive
 * @param ate último dia do período, inclusive
 * @param profissionalId colaborador que conduziu a atividade
 * @param pessoaId usuário atendido que participou
 */
record FiltroAtividades(TipoAtividade tipo, LocalDate de, LocalDate ate, UUID profissionalId, UUID pessoaId) {

	boolean periodoInvertido() {
		return de != null && ate != null && ate.isBefore(de);
	}

	Specification<Atividade> especificacao() {
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
			if (profissionalId != null) {
				condicoes.add(cb.isMember(profissionalId, raiz.get("profissionais")));
			}
			if (pessoaId != null) {
				condicoes.add(cb.isMember(pessoaId, raiz.get("participantes")));
			}
			return cb.and(condicoes.toArray(Predicate[]::new));
		};
	}

}
