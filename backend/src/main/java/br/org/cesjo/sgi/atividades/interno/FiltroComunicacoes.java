package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import br.org.cesjo.sgi.atividades.DirecaoComunicacao;
import br.org.cesjo.sgi.atividades.TipoComunicacao;

/**
 * Filtros da listagem de contatos e comunicações. Todos opcionais; o período inclui as duas pontas e
 * {@code contraparte} acha a pessoa ou instituição por trecho do nome, sem diferença de maiúsculas.
 */
record FiltroComunicacoes(TipoComunicacao tipo, DirecaoComunicacao direcao, LocalDate inicio, LocalDate fim,
		String contraparte) {

	Specification<Comunicacao> especificacao() {
		return (raiz, consulta, cb) -> {
			List<Predicate> condicoes = new ArrayList<>();
			if (tipo != null) {
				condicoes.add(cb.equal(raiz.get("tipo"), tipo));
			}
			if (direcao != null) {
				condicoes.add(cb.equal(raiz.get("direcao"), direcao));
			}
			if (inicio != null) {
				condicoes.add(cb.greaterThanOrEqualTo(raiz.get("data"), inicio));
			}
			if (fim != null) {
				condicoes.add(cb.lessThanOrEqualTo(raiz.get("data"), fim));
			}
			if (contraparte != null && !contraparte.isBlank()) {
				condicoes.add(cb.like(cb.lower(raiz.get("contraparte")), "%" + escapar(contraparte) + "%", '\\'));
			}
			return cb.and(condicoes.toArray(Predicate[]::new));
		};
	}

	/** Trecho em minúsculas, com os curingas do {@code like} tratados como texto. */
	private static String escapar(String texto) {
		return texto.strip()
			.toLowerCase(Locale.ROOT)
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_");
	}

}
