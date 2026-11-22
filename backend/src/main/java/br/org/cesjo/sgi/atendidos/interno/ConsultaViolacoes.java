package br.org.cesjo.sgi.atendidos.interno;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.atendidos.AmbitoViolacao;
import br.org.cesjo.sgi.atendidos.TipoViolacao;

/**
 * Consulta de leitura das violações de direitos (RF009, ADR 0145), por pessoa ou geral, filtrando e
 * paginando no banco. Junta {@code pessoa} só para o nome, como {@link ConsultaAtendidos}. Não lê a
 * descrição.
 */
@Component
class ConsultaViolacoes {

	/** A inserção mais recente da pessoa (a ativa primeiro) leva à ficha do usuário atendido. */
	private static final String BASE = """
			from violacao_direito v
			join pessoa p on p.id = v.pessoa_id
			left join lateral (
			    select i.id from insercao i where i.pessoa_id = v.pessoa_id
			    order by (i.situacao = 'ATIVA') desc, i.data_insercao desc, i.id limit 1
			) i on true
			""";

	private static final String COLUNAS = """
			select v.id, i.id as insercao_id, v.pessoa_id, p.nome_completo, v.tipo, v.ambito, v.data_identificacao
			""";

	/** Mais recente primeiro; o id desempata para a paginação ser estável. */
	private static final String ORDEM = " order by v.data_identificacao desc, v.criado_em desc, v.id";

	private final JdbcClient jdbc;

	ConsultaViolacoes(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	List<LinhaViolacao> daPessoa(UUID pessoaId, FiltroViolacoes filtro) {
		Map<String, Object> parametros = new LinkedHashMap<>();
		String onde = condicoes(pessoaId, filtro, parametros);
		return jdbc.sql(COLUNAS + BASE + onde + ORDEM).params(parametros).query(ConsultaViolacoes::linha).list();
	}

	Page<LinhaViolacao> todas(FiltroViolacoes filtro, Pageable pagina) {
		Map<String, Object> parametros = new LinkedHashMap<>();
		String onde = condicoes(null, filtro, parametros);
		long total = jdbc.sql("select count(*) from violacao_direito v" + onde)
			.params(parametros)
			.query(Long.class)
			.single();
		if (total == 0) {
			return Page.empty(pagina);
		}
		parametros.put("limite", pagina.getPageSize());
		parametros.put("deslocamento", pagina.getOffset());
		List<LinhaViolacao> linhas = jdbc.sql(COLUNAS + BASE + onde + ORDEM + " limit :limite offset :deslocamento")
			.params(parametros)
			.query(ConsultaViolacoes::linha)
			.list();
		return new PageImpl<>(linhas, pagina, total);
	}

	private static String condicoes(UUID pessoaId, FiltroViolacoes filtro, Map<String, Object> parametros) {
		List<String> condicoes = new ArrayList<>();
		if (pessoaId != null) {
			condicoes.add("v.pessoa_id = :pessoa");
			parametros.put("pessoa", pessoaId);
		}
		if (filtro.tipo() != null) {
			condicoes.add("v.tipo = :tipo");
			parametros.put("tipo", filtro.tipo().name());
		}
		if (filtro.de() != null) {
			condicoes.add("v.data_identificacao >= :de");
			parametros.put("de", filtro.de());
		}
		if (filtro.ate() != null) {
			condicoes.add("v.data_identificacao <= :ate");
			parametros.put("ate", filtro.ate());
		}
		return condicoes.isEmpty() ? "" : " where " + String.join(" and ", condicoes);
	}

	private static LinhaViolacao linha(ResultSet rs, int numero) throws SQLException {
		String ambito = rs.getString("ambito");
		return new LinhaViolacao(rs.getObject("id", UUID.class), rs.getObject("insercao_id", UUID.class),
				rs.getObject("pessoa_id", UUID.class), rs.getString("nome_completo"),
				TipoViolacao.valueOf(rs.getString("tipo")), ambito == null ? null : AmbitoViolacao.valueOf(ambito),
				rs.getObject("data_identificacao", LocalDate.class));
	}

}
