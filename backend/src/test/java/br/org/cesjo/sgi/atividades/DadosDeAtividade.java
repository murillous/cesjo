package br.org.cesjo.sgi.atividades;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Grava direto no banco usuários atendidos e colaboradores fictícios para os testes de atividades. O
 * Postgres dos testes é compartilhado, então cada teste cria os próprios registros (ADR 0008).
 */
@TestComponent
class DadosDeAtividade {

	private final JdbcTemplate jdbc;

	DadosDeAtividade(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** Pessoa fictícia inserida no serviço, na situação dada ({@code ATIVA} ou {@code DESLIGADA}). */
	UUID usuario(String nome, String situacao) {
		UUID pessoa = UUID.randomUUID();
		UUID inscricao = UUID.randomUUID();
		UUID responsavel = UUID.randomUUID();
		Timestamp agora = Timestamp.from(Instant.now());
		java.sql.Date hoje = java.sql.Date.valueOf(LocalDate.now());
		jdbc.update("""
				insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)
				values (?, ?, 'TEA', '31987654321', ?, ?)""", pessoa, nome, agora, agora);
		jdbc.update("""
				insert into responsavel (id, pessoa_id, nome_completo, criado_em, atualizado_em)
				values (?, ?, 'Responsável Fictício', ?, ?)""", responsavel, pessoa, agora, agora);
		jdbc.update("""
				insert into inscricao_lista_espera
				    (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao, criado_em, atualizado_em)
				values (?, ?, ?, 'CRAS', 'INSERIDO', ?, ?)""", inscricao, pessoa, hoje, agora, agora);
		jdbc.update("""
				insert into insercao (id, pessoa_id, inscricao_id, data_insercao, turno, cuidador_responsavel_id,
				    situacao, criado_em, atualizado_em)
				values (?, ?, ?, ?, 'MATUTINO', ?, ?, ?, ?)""", UUID.randomUUID(), pessoa, inscricao, hoje,
				responsavel, situacao, agora, agora);
		return pessoa;
	}

	UUID colaborador(String nome, boolean ativo) {
		UUID id = UUID.randomUUID();
		Timestamp agora = Timestamp.from(Instant.now());
		jdbc.update("""
				insert into colaborador (id, nome, funcao, ativo, criado_em, atualizado_em)
				values (?, ?, 'PSICOLOGO', ?, ?, ?)""", id, nome, ativo, agora, agora);
		return id;
	}

	/** Valor gravado na coluna, sem decifrar. */
	String descricaoNoBanco(String atividade) {
		return jdbc.queryForObject("select descricao from atividade where id = ?::uuid", String.class, atividade);
	}

}
