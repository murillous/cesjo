package br.org.cesjo.sgi.auditoria.interno;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.org.cesjo.sgi.auditoria.AcaoAuditoria;

/**
 * Filtros da consulta do log de auditoria (ADR 0024): {@code de} inclusivo, {@code ate} exclusivo,
 * {@code login} sem diferenciar maiúsculas, {@code entidade} e {@code acao} exatos. O mesmo filtro
 * vira a {@link Specification} da página e o SQL da contagem limitada (ADR 0236).
 */
record FiltroAuditoria(Instant de, Instant ate, String login, String entidade, AcaoAuditoria acao) {

	FiltroAuditoria {
		login = temTexto(login) ? login.strip().toLowerCase(Locale.ROOT) : null;
		entidade = temTexto(entidade) ? entidade.strip() : null;
	}

	Specification<RegistroAuditoria> especificacao() {
		return (raiz, consulta, cb) -> {
			List<Predicate> condicoes = new ArrayList<>();
			if (de != null) {
				condicoes.add(cb.greaterThanOrEqualTo(raiz.get("ocorridoEm"), de));
			}
			if (ate != null) {
				condicoes.add(cb.lessThan(raiz.get("ocorridoEm"), ate));
			}
			if (login != null) {
				condicoes.add(cb.equal(cb.lower(raiz.get("contaLogin")), login));
			}
			if (entidade != null) {
				condicoes.add(cb.equal(raiz.get("entidade"), entidade));
			}
			if (acao != null) {
				condicoes.add(cb.equal(raiz.get("acao"), acao));
			}
			return cb.and(condicoes.toArray(Predicate[]::new));
		};
	}

	/**
	 * Conta os registros do filtro até {@code teto + 1}: a consulta para de ler assim que passa do teto,
	 * então o custo não cresce com o tamanho do log.
	 */
	long contarAte(JdbcClient jdbc, long teto) {
		List<String> condicoes = new ArrayList<>();
		Map<String, Object> parametros = new LinkedHashMap<>();
		if (de != null) {
			condicoes.add("ocorrido_em >= :de");
			parametros.put("de", Timestamp.from(de));
		}
		if (ate != null) {
			condicoes.add("ocorrido_em < :ate");
			parametros.put("ate", Timestamp.from(ate));
		}
		if (login != null) {
			condicoes.add("lower(conta_login) = :login");
			parametros.put("login", login);
		}
		if (entidade != null) {
			condicoes.add("entidade = :entidade");
			parametros.put("entidade", entidade);
		}
		if (acao != null) {
			condicoes.add("acao = :acao");
			parametros.put("acao", acao.name());
		}
		String onde = condicoes.isEmpty() ? "" : " where " + String.join(" and ", condicoes);
		parametros.put("limite", teto + 1);
		return jdbc.sql("select count(*) from (select 1 from auditoria" + onde + " limit :limite) as limitada")
			.params(parametros)
			.query(Long.class)
			.single();
	}

	private static boolean temTexto(String texto) {
		return texto != null && !texto.isBlank();
	}

}
