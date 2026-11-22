package br.org.cesjo.sgi.listaespera.interno;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.listaespera.MotivoSaida;

/**
 * Contagens da lista de espera por período para os relatórios estatísticos (RF016, ADR 0200), em SQL
 * agregado sobre as tabelas do próprio módulo. Só conta: não lê nome, contato nem observação.
 */
@Component
class ContagensListaEspera {

	private final JdbcClient jdbc;

	ContagensListaEspera(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	long inscricoes(LocalDate inicio, LocalDate fim) {
		return jdbc.sql("select count(*) from inscricao_lista_espera where data_cadastro between :inicio and :fim")
			.param("inicio", inicio)
			.param("fim", fim)
			.query(Long.class)
			.single();
	}

	/**
	 * Inscrições na fila (aguardando ou em avaliação) no fim do dia: a situação de cada uma é a da
	 * última mudança do histórico com data até o dia.
	 */
	long naFilaEm(LocalDate dia) {
		return jdbc.sql("""
				select count(*) from inscricao_lista_espera i
				join lateral (
				    select h.situacao_nova from historico_situacao_inscricao h
				    where h.inscricao_id = i.id and h.data <= :dia
				    order by h.data desc, h.registrado_em desc limit 1
				) ultima on true
				where i.data_cadastro <= :dia and ultima.situacao_nova in ('AGUARDANDO', 'EM_AVALIACAO')
				""").param("dia", dia).query(Long.class).single();
	}

	Map<MotivoSaida, Long> saidasPorMotivo(LocalDate inicio, LocalDate fim) {
		Map<MotivoSaida, Long> contagem = new EnumMap<>(MotivoSaida.class);
		for (MotivoSaida motivo : MotivoSaida.values()) {
			contagem.put(motivo, 0L);
		}
		jdbc.sql("""
				select motivo_saida, count(*) as total from historico_situacao_inscricao
				where situacao_nova = 'SAIU' and data between :inicio and :fim group by motivo_saida
				""")
			.param("inicio", inicio)
			.param("fim", fim)
			.query((rs, n) -> contagem.put(MotivoSaida.valueOf(rs.getString("motivo_saida")), rs.getLong("total")))
			.list();
		return contagem;
	}

}
