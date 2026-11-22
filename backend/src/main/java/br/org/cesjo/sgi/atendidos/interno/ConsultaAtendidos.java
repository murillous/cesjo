package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.atendidos.SituacaoInsercao;
import br.org.cesjo.sgi.atendidos.Turno;
import br.org.cesjo.sgi.pessoas.Genero;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Consulta de leitura da listagem de usuários atendidos (ADR 0083). Junta {@code insercao},
 * {@code pessoa} e {@code cadastro_complementar} num SQL só, para filtrar e paginar no banco. É a única
 * leitura das tabelas de outro módulo, sempre só de leitura; toda gravação passa pelas APIs Java. Só lê
 * colunas em claro (ADR 0141).
 */
@Component
class ConsultaAtendidos {

	private static final String BASE = """
			from insercao i
			join pessoa p on p.id = i.pessoa_id
			left join cadastro_complementar c on c.pessoa_id = p.id
			left join desligamento d on d.insercao_id = i.id
			""";

	private final JdbcClient jdbc;

	ConsultaAtendidos(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Página em ordem de nome; o id desempata nomes iguais para a paginação ser estável. */
	Page<LinhaAtendido> listar(FiltroAtendidos filtro, LocalDate hoje, Pageable pagina) {
		Map<String, Object> parametros = new LinkedHashMap<>();
		String onde = condicoes(filtro, hoje, parametros);

		long total = jdbc.sql("select count(*) " + BASE + onde).params(parametros).query(Long.class).single();
		if (total == 0) {
			return Page.empty(pagina);
		}
		parametros.put("limite", pagina.getPageSize());
		parametros.put("deslocamento", pagina.getOffset());
		List<LinhaAtendido> linhas = jdbc
			.sql("""
					select i.id, i.pessoa_id, p.nome_completo, p.data_nascimento, p.tipo_deficiencia,
					       i.turno, i.situacao, i.data_insercao, d.data as data_desligamento,
					       c.genero, c.bairro
					""" + BASE + onde + " order by lower(p.nome_completo), i.id limit :limite offset :deslocamento")
			.params(parametros)
			.query((rs, n) -> new LinhaAtendido(rs.getObject("id", UUID.class), rs.getObject("pessoa_id", UUID.class),
					rs.getString("nome_completo"), rs.getObject("data_nascimento", LocalDate.class),
					TipoDeficiencia.valueOf(rs.getString("tipo_deficiencia")), Turno.valueOf(rs.getString("turno")),
					SituacaoInsercao.valueOf(rs.getString("situacao")), rs.getObject("data_insercao", LocalDate.class),
					rs.getObject("data_desligamento", LocalDate.class),
					rs.getString("genero") == null ? null : Genero.valueOf(rs.getString("genero")),
					rs.getString("bairro")))
			.list();
		return new PageImpl<>(linhas, pagina, total);
	}

	private static String condicoes(FiltroAtendidos filtro, LocalDate hoje, Map<String, Object> parametros) {
		List<String> condicoes = new ArrayList<>();
		if (filtro.nome() != null && !filtro.nome().isBlank()) {
			condicoes.add("lower(p.nome_completo) like :nome escape '\\'");
			parametros.put("nome", "%" + semCuringas(filtro.nome().strip().toLowerCase(Locale.ROOT)) + "%");
		}
		if (filtro.turno() != null) {
			condicoes.add("i.turno = :turno");
			parametros.put("turno", filtro.turno().name());
		}
		if (filtro.situacao() != null) {
			condicoes.add("i.situacao = :situacao");
			parametros.put("situacao", filtro.situacao().name());
		}
		if (filtro.tipoDeficiencia() != null) {
			condicoes.add("p.tipo_deficiencia = :tipo");
			parametros.put("tipo", filtro.tipoDeficiencia().name());
		}
		if (filtro.genero() != null) {
			condicoes.add("c.genero = :genero");
			parametros.put("genero", filtro.genero().name());
		}
		if (filtro.bairro() != null && !filtro.bairro().isBlank()) {
			condicoes.add("lower(c.bairro) like :bairro escape '\\'");
			parametros.put("bairro", "%" + semCuringas(filtro.bairro().strip().toLowerCase(Locale.ROOT)) + "%");
		}
		if (filtro.faixaEtaria() != null) {
			condicoes.add("p.data_nascimento <= :nascidoAte and p.data_nascimento > :nascidoDepoisDe");
			parametros.put("nascidoAte", filtro.faixaEtaria().nascidoAte(hoje));
			parametros.put("nascidoDepoisDe", filtro.faixaEtaria().nascidoDepoisDe(hoje));
		}
		return condicoes.isEmpty() ? "" : " where " + String.join(" and ", condicoes);
	}

	/** O texto digitado é procurado literalmente: {@code %} e {@code _} não viram curingas. */
	private static String semCuringas(String texto) {
		return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
