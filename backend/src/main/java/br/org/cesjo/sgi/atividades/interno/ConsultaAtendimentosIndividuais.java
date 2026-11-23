package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.atividades.AtendimentosIndividuais;
import br.org.cesjo.sgi.atividades.TipoAtividade;

/** Contagem dos atendimentos individuais por pessoa (ADR 0205), em SQL agregado nas tabelas do módulo. */
@Service
class ConsultaAtendimentosIndividuais implements AtendimentosIndividuais {

	private static final List<String> TIPOS_INDIVIDUAIS = Arrays.stream(TipoAtividade.values())
		.filter(TipoAtividade::individual)
		.map(Enum::name)
		.toList();

	private final JdbcClient jdbc;

	ConsultaAtendimentosIndividuais(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, Resumo> resumirPorPessoa(Collection<UUID> pessoaIds, LocalDate inicio, LocalDate fim) {
		if (fim.isBefore(inicio)) {
			throw new IllegalArgumentException("O fim do período não pode ser anterior ao início.");
		}
		Map<UUID, Resumo> resumos = new HashMap<>();
		if (pessoaIds.isEmpty()) {
			return resumos;
		}
		jdbc.sql("""
				select ap.pessoa_id, count(*) as quantidade, max(a.data_atividade) as ultimo
				from atividade a
				join atividade_participante ap on ap.atividade_id = a.id
				where a.tipo in (:tipos) and a.data_atividade between :inicio and :fim and ap.pessoa_id in (:pessoas)
				group by ap.pessoa_id
				""")
			.param("tipos", TIPOS_INDIVIDUAIS)
			.param("inicio", inicio)
			.param("fim", fim)
			.param("pessoas", List.copyOf(pessoaIds))
			.query((rs, n) -> resumos.put(rs.getObject("pessoa_id", UUID.class),
					new Resumo(rs.getLong("quantidade"), rs.getObject("ultimo", LocalDate.class))))
			.list();
		return resumos;
	}

}
