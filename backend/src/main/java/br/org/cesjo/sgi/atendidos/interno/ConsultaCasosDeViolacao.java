package br.org.cesjo.sgi.atendidos.interno;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.atendidos.AmbitoViolacao;
import br.org.cesjo.sgi.atendidos.CasoDeViolacao;
import br.org.cesjo.sgi.atendidos.CasosDeViolacao;
import br.org.cesjo.sgi.atendidos.MotivoDesligamento;
import br.org.cesjo.sgi.atendidos.TipoViolacao;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Casos de violação por período (ADR 0205), em SQL sobre as tabelas do próprio módulo. Junta
 * {@code pessoa} só para o nome. Não lê a descrição, que é cifrada e tem leitura auditada à parte
 * (ADR 0145). A situação no serviço é a da inserção mais recente até o fim do período.
 */
@Service
class ConsultaCasosDeViolacao implements CasosDeViolacao {

	private static final String SQL = """
			select v.pessoa_id, p.nome_completo, v.tipo, v.ambito, v.data_identificacao,
			       i.id as insercao_id, d.data as data_desligamento, d.motivo
			from violacao_direito v
			join pessoa p on p.id = v.pessoa_id
			left join lateral (
			    select i.id from insercao i where i.pessoa_id = v.pessoa_id and i.data_insercao <= :fim
			    order by i.data_insercao desc, i.id limit 1
			) i on true
			left join desligamento d on d.insercao_id = i.id and d.data <= :fim
			where v.data_identificacao between :inicio and :fim
			order by p.nome_completo, v.pessoa_id, v.data_identificacao, v.criado_em, v.id
			""";

	private final JdbcClient jdbc;

	ConsultaCasosDeViolacao(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CasoDeViolacao> casosNoPeriodo(LocalDate inicio, LocalDate fim) {
		Objects.requireNonNull(inicio, "inicio");
		Objects.requireNonNull(fim, "fim");
		if (fim.isBefore(inicio)) {
			throw new RegraNegocioException("A data final do período não pode ser anterior à inicial.");
		}
		List<Linha> linhas = jdbc.sql(SQL)
			.param("inicio", inicio)
			.param("fim", fim)
			.query(ConsultaCasosDeViolacao::linha)
			.list();
		// As linhas vêm ordenadas por pessoa: cada pessoa vira um caso com as suas violações.
		Map<UUID, List<Linha>> porPessoa = new LinkedHashMap<>();
		for (Linha linha : linhas) {
			porPessoa.computeIfAbsent(linha.pessoaId(), id -> new ArrayList<>()).add(linha);
		}
		List<CasoDeViolacao> casos = new ArrayList<>(porPessoa.size());
		porPessoa.forEach((pessoaId, daPessoa) -> {
			Linha primeira = daPessoa.getFirst();
			casos.add(new CasoDeViolacao(pessoaId, primeira.nome(),
					daPessoa.stream().map(Linha::violacao).toList(), primeira.inserido(), primeira.dataDesligamento(),
					primeira.motivo()));
		});
		return casos;
	}

	private static Linha linha(ResultSet rs, int numero) throws SQLException {
		String ambito = rs.getString("ambito");
		String motivo = rs.getString("motivo");
		return new Linha(rs.getObject("pessoa_id", UUID.class), rs.getString("nome_completo"),
				new CasoDeViolacao.Violacao(TipoViolacao.valueOf(rs.getString("tipo")),
						ambito == null ? null : AmbitoViolacao.valueOf(ambito),
						rs.getObject("data_identificacao", LocalDate.class)),
				rs.getObject("insercao_id") != null, rs.getObject("data_desligamento", LocalDate.class),
				motivo == null ? null : MotivoDesligamento.valueOf(motivo));
	}

	private record Linha(UUID pessoaId, String nome, CasoDeViolacao.Violacao violacao, boolean inserido,
			LocalDate dataDesligamento, MotivoDesligamento motivo) {
	}

}
