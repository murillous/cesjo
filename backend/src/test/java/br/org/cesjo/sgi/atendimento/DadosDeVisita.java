package br.org.cesjo.sgi.atendimento;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Grava direto no banco candidatos e colaboradores fictícios para os testes de visitas. O Postgres dos
 * testes é compartilhado, então cada teste cria os próprios registros (ADR 0008).
 */
@TestComponent
class DadosDeVisita {

	private final JdbcTemplate jdbc;

	DadosDeVisita(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** Pessoa fictícia com inscrição na situação dada. Devolve o {@code pessoa_id}. */
	UUID candidato(String nome, String situacao) {
		UUID pessoa = UUID.randomUUID();
		Timestamp agora = Timestamp.from(Instant.now());
		jdbc.update("""
				insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)
				values (?, ?, 'TEA', '31987654321', ?, ?)""", pessoa, nome, agora, agora);
		jdbc.update("""
				insert into inscricao_lista_espera
				    (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao, criado_em, atualizado_em)
				values (?, ?, ?, 'CRAS', ?, ?, ?)""", UUID.randomUUID(), pessoa, java.sql.Date.valueOf(LocalDate.now()),
				situacao, agora, agora);
		return pessoa;
	}

	/** Pessoa fictícia sem inscrição na lista de espera. */
	UUID pessoaSemInscricao() {
		UUID pessoa = UUID.randomUUID();
		Timestamp agora = Timestamp.from(Instant.now());
		jdbc.update("""
				insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)
				values (?, 'Pessoa Sem Inscrição', 'FISICA', '31987654321', ?, ?)""", pessoa, agora, agora);
		return pessoa;
	}

	UUID colaborador(String nome, boolean ativo) {
		UUID id = UUID.randomUUID();
		Timestamp agora = Timestamp.from(Instant.now());
		jdbc.update("""
				insert into colaborador (id, nome, funcao, ativo, criado_em, atualizado_em)
				values (?, ?, 'ASSISTENTE_SOCIAL', ?, ?, ?)""", id, nome, ativo, agora, agora);
		return id;
	}

	/** Visita agendada gravada direto, inclusive em dia passado, que a API não deixa agendar. */
	UUID visita(UUID pessoa, java.time.LocalDateTime dataHora, UUID profissional) {
		UUID id = UUID.randomUUID();
		Timestamp agora = Timestamp.from(Instant.now());
		jdbc.update("""
				insert into visita_domiciliar (id, pessoa_id, data_hora, status, criado_em, atualizado_em)
				values (?, ?, ?, 'AGENDADA', ?, ?)""", id, pessoa, Timestamp.valueOf(dataHora), agora, agora);
		jdbc.update("insert into visita_profissional (visita_id, colaborador_id) values (?, ?)", id, profissional);
		return id;
	}

	/** Valor gravado na coluna, sem decifrar. */
	String observacoesNoBanco(UUID visita) {
		return jdbc.queryForObject("select observacoes from visita_domiciliar where id = ?", String.class, visita);
	}

	String situacao(UUID pessoa) {
		return jdbc.queryForObject("select situacao from inscricao_lista_espera where pessoa_id = ?", String.class,
				pessoa);
	}

}
