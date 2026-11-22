package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.atendidos.MotivoDesligamento;
import br.org.cesjo.sgi.atendidos.TipoViolacao;

/**
 * Contagens por período para os dashboards e relatórios (RF016, RF017, ADR 0198), em SQL agregado
 * sobre as tabelas do próprio módulo. Só conta: não lê nome, descrição nem observação.
 */
@Component
class ContagensDoPeriodo {

	private final JdbcClient jdbc;

	ContagensDoPeriodo(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	Map<TipoViolacao, Long> violacoesPorTipo(LocalDate inicio, LocalDate fim) {
		Map<TipoViolacao, Long> contagem = zerado(TipoViolacao.class);
		jdbc.sql("""
				select tipo, count(*) as total from violacao_direito
				where data_identificacao between :inicio and :fim group by tipo
				""")
			.param("inicio", inicio)
			.param("fim", fim)
			.query((rs, n) -> contagem.put(TipoViolacao.valueOf(rs.getString("tipo")), rs.getLong("total")))
			.list();
		return contagem;
	}

	Map<MotivoDesligamento, Long> desligamentosPorMotivo(LocalDate inicio, LocalDate fim) {
		Map<MotivoDesligamento, Long> contagem = zerado(MotivoDesligamento.class);
		jdbc.sql("""
				select motivo, count(*) as total from desligamento
				where data between :inicio and :fim group by motivo
				""")
			.param("inicio", inicio)
			.param("fim", fim)
			.query((rs, n) -> contagem.put(MotivoDesligamento.valueOf(rs.getString("motivo")), rs.getLong("total")))
			.list();
		return contagem;
	}

	/**
	 * Inserções que estiveram ativas em algum dia do período: inseridas até o fim e sem desligamento
	 * antes do início.
	 */
	long atendidosNoPeriodo(LocalDate inicio, LocalDate fim) {
		return jdbc.sql("""
				select count(*) from insercao i
				left join desligamento d on d.insercao_id = i.id
				where i.data_insercao <= :fim and (d.id is null or d.data >= :inicio)
				""").param("inicio", inicio).param("fim", fim).query(Long.class).single();
	}

	long insercoes(LocalDate inicio, LocalDate fim) {
		return jdbc.sql("select count(*) from insercao where data_insercao between :inicio and :fim")
			.param("inicio", inicio)
			.param("fim", fim)
			.query(Long.class)
			.single();
	}

	long ativosEm(LocalDate dia) {
		return jdbc.sql("""
				select count(*) from insercao i
				left join desligamento d on d.insercao_id = i.id
				where i.data_insercao <= :dia and (d.id is null or d.data > :dia)
				""").param("dia", dia).query(Long.class).single();
	}

	private static <E extends Enum<E>> Map<E, Long> zerado(Class<E> tipo) {
		Map<E, Long> mapa = new EnumMap<>(tipo);
		for (E valor : tipo.getEnumConstants()) {
			mapa.put(valor, 0L);
		}
		return mapa;
	}

}
