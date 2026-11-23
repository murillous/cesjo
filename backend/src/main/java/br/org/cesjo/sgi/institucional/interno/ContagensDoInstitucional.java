package br.org.cesjo.sgi.institucional.interno;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.institucional.ContagensInstitucionais;
import br.org.cesjo.sgi.institucional.DirecaoDoacao;
import br.org.cesjo.sgi.institucional.FuncaoColaborador;
import br.org.cesjo.sgi.institucional.TipoDoacao;
import br.org.cesjo.sgi.institucional.TipoEventoInstitucional;

/**
 * Contagens do módulo para o relatório de transparência (RF016, ADR 0211), em SQL agregado sobre as
 * tabelas do próprio módulo. Só conta e soma: não lê nome, descrição, contraparte nem motivo.
 */
@Component
class ContagensDoInstitucional implements ContagensInstitucionais {

	private final JdbcClient jdbc;

	ContagensDoInstitucional(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Map<FuncaoColaborador, Long> contarColaboradoresPorFuncaoEm(LocalDate dia) {
		Map<FuncaoColaborador, Long> contagem = zerado(FuncaoColaborador.class);
		jdbc.sql("""
				select c.funcao, count(*) as total from colaborador c
				where exists (
				    select 1 from vinculo_colaborador v
				    where v.colaborador_id = c.id and v.data_contratacao <= :dia
				      and (v.data_desligamento is null or v.data_desligamento > :dia))
				   or (c.ativo and not exists (select 1 from vinculo_colaborador v where v.colaborador_id = c.id))
				group by c.funcao
				""")
			.param("dia", dia)
			.query((rs, n) -> contagem.put(FuncaoColaborador.valueOf(rs.getString("funcao")), rs.getLong("total")))
			.list();
		return contagem;
	}

	@Override
	public long contarParceriasVigentes(LocalDate inicio, LocalDate fim) {
		return jdbc.sql("""
				select count(*) from parceiro
				where ativo
				  and (vigencia_inicio is null or vigencia_inicio <= :fim)
				  and (vigencia_fim is null or vigencia_fim >= :inicio)
				""").param("inicio", inicio).param("fim", fim).query(Long.class).single();
	}

	@Override
	public long contarArticulacoes(LocalDate inicio, LocalDate fim) {
		return jdbc.sql("select count(*) from articulacao_parceria where data between :inicio and :fim")
			.param("inicio", inicio)
			.param("fim", fim)
			.query(Long.class)
			.single();
	}

	@Override
	public Map<TipoEventoInstitucional, Long> contarEventosPorTipo(LocalDate inicio, LocalDate fim) {
		Map<TipoEventoInstitucional, Long> contagem = zerado(TipoEventoInstitucional.class);
		jdbc.sql("""
				select tipo, count(*) as total from evento_institucional
				where data between :inicio and :fim group by tipo
				""")
			.param("inicio", inicio)
			.param("fim", fim)
			.query((rs, n) -> contagem.put(TipoEventoInstitucional.valueOf(rs.getString("tipo")), rs.getLong("total")))
			.list();
		return contagem;
	}

	@Override
	public Map<TipoDoacao, Long> contarDoacoesPorTipo(DirecaoDoacao direcao, LocalDate inicio, LocalDate fim) {
		Map<TipoDoacao, Long> contagem = zerado(TipoDoacao.class);
		jdbc.sql("""
				select tipo, count(*) as total from doacao
				where direcao = :direcao and data between :inicio and :fim group by tipo
				""")
			.param("direcao", direcao.name())
			.param("inicio", inicio)
			.param("fim", fim)
			.query((rs, n) -> contagem.put(TipoDoacao.valueOf(rs.getString("tipo")), rs.getLong("total")))
			.list();
		return contagem;
	}

	@Override
	public BigDecimal somarDoacoesEmDinheiro(DirecaoDoacao direcao, LocalDate inicio, LocalDate fim) {
		BigDecimal soma = jdbc.sql("""
				select coalesce(sum(valor), 0) from doacao
				where direcao = :direcao and tipo = 'DINHEIRO' and data between :inicio and :fim
				""")
			.param("direcao", direcao.name())
			.param("inicio", inicio)
			.param("fim", fim)
			.query(BigDecimal.class)
			.single();
		return soma.setScale(2, RoundingMode.HALF_UP);
	}

	private static <E extends Enum<E>> Map<E, Long> zerado(Class<E> tipo) {
		Map<E, Long> mapa = new EnumMap<>(tipo);
		for (E valor : tipo.getEnumConstants()) {
			mapa.put(valor, 0L);
		}
		return mapa;
	}

}
