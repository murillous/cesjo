package br.org.cesjo.sgi.desempenho;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Massa de dados fictícios do teste de desempenho (issue #25, RNF 3.2, ADR 0230). Grava direto no banco
 * com {@code generate_series}, sem passar pelas APIs, para gerar centenas de milhares de linhas em
 * segundos. Os ids são derivados do número da linha ({@code md5('pessoa' || n)::uuid}), o que permite
 * ligar as tabelas sem tabela temporária.
 *
 * <p>
 * As colunas cifradas (ADR 0004) recebem um único valor cifrado de verdade pelo {@code Cifrador} do
 * projeto, reaproveitado em todas as linhas: as listagens e os relatórios não decifram esse texto, mas a
 * coluna fica com o tamanho e o formato reais.
 */
final class MassaSimulada {

	/**
	 * Volume esperado em 5 anos de uso, base do fator de multiplicação (ADR 0230).
	 */
	// PENDENTE-CESJO(P40): volume esperado estimado sem número da equipe CESJO (ADR 0230).
	static final Volume ESPERADO = new Volume(5, 600, 200, 300, 30, 6_000, 4, 6_000, 200_000, 40, 500,
			500, 1_000);

	/** Quantidades de uma massa; {@code pessoas} inclui fila, inseridos e quem saiu da lista. */
	record Volume(int anos, int pessoas, int insercoes, int violacoes, int colaboradores, int atividades,
			int participantesPorAtividade, int comunicacoes, int auditoria, int parceiros, int articulacoes,
			int eventos, int doacoes) {

		Volume vezes(int fator) {
			return new Volume(anos, pessoas * fator, insercoes * fator, violacoes * fator, colaboradores * fator,
					atividades * fator, participantesPorAtividade, comunicacoes * fator, auditoria * fator,
					parceiros * fator, articulacoes * fator, eventos * fator, doacoes * fator);
		}

	}

	private final JdbcTemplate jdbc;

	private final String cifrado;

	MassaSimulada(JdbcTemplate jdbc, String textoCifrado) {
		this.jdbc = jdbc;
		this.cifrado = textoCifrado;
	}

	/**
	 * Grava a massa e roda {@code vacuum analyze}. Devolve o total de linhas por tabela.
	 *
	 * <p>
	 * Distribuição: a pessoa {@code n} é inserida no serviço quando {@code n % 3 = 0} (um terço), e
	 * 30% dos inseridos seguem ativos; das demais, 3 em 8 seguem na fila e o resto saiu da lista. As
	 * datas se espalham pelos {@code anos} até hoje.
	 */
	Map<String, Long> gravar(Volume v, LocalDate hoje) {
		int p = v.pessoas();
		int inseridos = p / 3;
		int dias = v.anos() * 365;
		String inicio = "date '" + hoje.minusDays(dias) + "'";
		String limite = "date '" + hoje + "'";

		executar("""
				insert into pessoa (id, nome_completo, data_nascimento, tipo_deficiencia, telefone, criado_em,
				                    atualizado_em)
				select md5('pessoa' || n)::uuid,
				       'Pessoa Fictícia ' || n || ' ' || (array['Silva','Souza','Oliveira','Santos','Pereira','Lima'])[1 + n %% 6],
				       case when n %% 50 = 0 then null else date '1966-01-01' + (n * 37 %% 14600) end,
				       (array['FISICA','SENSORIAL','INTELECTUAL','MULTIPLA','TEA','T21'])[1 + n %% 6],
				       '319' || lpad(n::text, 8, '0'),
				       now(), now()
				from generate_series(1, %d) n""".formatted(p));
		executar("""
				insert into responsavel (id, pessoa_id, nome_completo, criado_em, atualizado_em)
				select md5('responsavel' || n)::uuid, md5('pessoa' || n)::uuid, 'Responsável Fictício ' || n, now(), now()
				from generate_series(1, %d) n""".formatted(p));

		// Cadastro da pessoa n em ordem crescente ao longo do período; a inserção 30 dias depois.
		String cadastro = "(%s + (n::bigint * %d / %d)::int)".formatted(inicio, dias, p);
		String insercao = "least(%s + 30, %s)".formatted(cadastro, limite);
		executar("""
				insert into inscricao_lista_espera (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao,
				                                    data_saida, motivo_saida, criado_em, atualizado_em)
				select md5('inscricao' || n)::uuid, md5('pessoa' || n)::uuid, %1$s,
				       (array['CRAS','CREAS','JUDICIARIO','SAUDE','EDUCACAO','DEMANDA_ESPONTANEA'])[1 + n %% 6],
				       s.situacao,
				       case when s.situacao = 'SAIU' then least(%1$s + 60, %2$s) end,
				       case when s.situacao = 'SAIU'
				            then (array['DESISTENCIA','NAO_LOCALIZADO','FORA_DO_PERFIL','OUTRO'])[1 + n %% 4] end,
				       now(), now()
				from generate_series(1, %3$d) n
				cross join lateral (select case when n %% 3 = 0 then 'INSERIDO'
				                                when n %% 8 < 3 and n %% 5 = 0 then 'EM_AVALIACAO'
				                                when n %% 8 < 3 then 'AGUARDANDO'
				                                else 'SAIU' end as situacao) s""".formatted(cadastro, limite, p));
		executar("""
				insert into historico_situacao_inscricao (id, inscricao_id, situacao_anterior, situacao_nova, data,
				                                          registrado_em)
				select md5('historico' || n)::uuid, md5('inscricao' || n)::uuid, null, 'AGUARDANDO', %s, now()
				from generate_series(1, %d) n""".formatted(cadastro, p));
		executar("""
				insert into historico_situacao_inscricao (id, inscricao_id, situacao_anterior, situacao_nova, data,
				                                          motivo_saida, observacao, registrado_em)
				select md5('historico-2-' || i.id)::uuid, i.id, 'AGUARDANDO', i.situacao,
				       coalesce(i.data_saida, i.data_cadastro), i.motivo_saida, ?, now()
				from inscricao_lista_espera i
				where i.situacao in ('INSERIDO', 'SAIU')""", cifrado);

		// Inserções: n = 3k. As desligadas têm desligamento 200 dias depois, no máximo hoje.
		executar("""
				insert into insercao (id, pessoa_id, inscricao_id, data_insercao, turno, cuidador_responsavel_id,
				                      situacao, criado_em, atualizado_em)
				select md5('insercao' || n)::uuid, md5('pessoa' || n)::uuid, md5('inscricao' || n)::uuid, %s,
				       (array['MATUTINO','VESPERTINO','INTEGRAL'])[1 + (n / 3) %% 3], md5('responsavel' || n)::uuid,
				       case when (n / 3) %% 10 < 3 then 'ATIVA' else 'DESLIGADA' end, now(), now()
				from generate_series(3, %d, 3) n""".formatted(insercao, p));
		executar("""
				insert into desligamento (id, insercao_id, data, motivo, observacao, criado_em, atualizado_em)
				select md5('desligamento' || i.id)::uuid, i.id, least(i.data_insercao + 200, %s),
				       (array['EVASAO','MUDANCA_MUNICIPIO','SOLICITACAO_FAMILIA','ALCANCE_OBJETIVOS','OUTROS'])
				           [1 + abs(hashtext(i.id::text)) %% 5],
				       case when abs(hashtext(i.id::text)) %% 2 = 0 then ? end, now(), now()
				from insercao i
				where i.situacao = 'DESLIGADA'""".formatted(limite), cifrado);
		executar("""
				insert into cadastro_complementar (pessoa_id, genero, escolaridade, cep, bairro, cidade,
				                                   recebe_bpc, inscrito_cad_unico, recebe_bolsa_familia,
				                                   crenca_religiosa, criado_em, atualizado_em)
				select md5('pessoa' || n)::uuid,
				       (array['FEMININO','MASCULINO','OUTRO',null])[1 + n %% 4],
				       (array['SEM_ESCOLARIDADE','FUNDAMENTAL_INCOMPLETO','MEDIO_COMPLETO','SUPERIOR_COMPLETO'])[1 + n %% 4],
				       '30000' || lpad((n %% 1000)::text, 3, '0'),
				       'Bairro Fictício ' || (n %% 40), 'Cidade Fictícia', n %% 2 = 0, n %% 3 = 0, n %% 5 = 0,
				       (array['CATOLICA','EVANGELICA','OUTRAS',null])[1 + n %% 4], now(), now()
				from generate_series(3, %d, 3) n""".formatted(p));

		// Documentos obrigatórios (laudo e comprovante) para metade das pessoas.
		executar("""
				insert into arquivo (id, nome_original, tipo_mime, tamanho, sha256, caminho_armazenamento, chave_id,
				                     enviado_em)
				select md5('arquivo-' || t || n)::uuid, ?, 'application/pdf', 1024, md5(t || n) || md5(n || t),
				       'desempenho/' || t || '-' || n, 'teste-1', now()
				from generate_series(2, %d, 2) n
				cross join unnest(array['LAUDO_MEDICO', 'COMPROVANTE_RESIDENCIA']) t""".formatted(p), cifrado);
		executar("""
				insert into documento_pessoa (id, pessoa_id, tipo, arquivo_id, criado_em, atualizado_em)
				select md5('documento-' || t || n)::uuid, md5('pessoa' || n)::uuid, t, md5('arquivo-' || t || n)::uuid,
				       now(), now()
				from generate_series(2, %d, 2) n
				cross join unnest(array['LAUDO_MEDICO', 'COMPROVANTE_RESIDENCIA']) t""".formatted(p));

		// Violações de pessoas inseridas, espalhadas pelo período.
		executar("""
				insert into violacao_direito (id, pessoa_id, tipo, ambito, data_identificacao, descricao, criado_em,
				                              atualizado_em)
				select md5('violacao' || k)::uuid, md5('pessoa' || (3 * (1 + k %% %d)))::uuid, t.tipo,
				       case when t.tipo in ('ABUSO_SEXUAL', 'VIOLENCIA_FISICA_PSICOLOGICA') then 'INTRAFAMILIAR' end,
				       %s + (k::bigint * %d / %d)::int, ?, now(), now()
				from generate_series(1, %d) k
				cross join lateral (select (array['EXPLORACAO_IMAGEM','ISOLAMENTO_SOCIAL','CONFINAMENTO',
				        'DISCRIMINACAO_FAMILIAR','FALTA_CUIDADOS_CUIDADOR','ESTRESSE_CUIDADOR','DESVALORIZACAO_CAPACIDADE',
				        'VIOLENCIA_PATRIMONIAL','NEGLIGENCIA_ABANDONO','ABUSO_SEXUAL','VIOLENCIA_FISICA_PSICOLOGICA',
				        'OUTRAS'])[1 + k %% 12] as tipo) t""".formatted(inseridos, inicio, dias, v.violacoes(),
				v.violacoes()), cifrado);

		executar("""
				insert into colaborador (id, nome, funcao, ativo, criado_em, atualizado_em)
				select md5('colaborador' || c)::uuid, 'Colaborador Fictício ' || c,
				       (array['ASSISTENTE_SOCIAL','PSICOLOGO','TERAPEUTA_OCUPACIONAL','OUTRA'])[1 + c %% 4], c %% 10 <> 0,
				       now(), now()
				from generate_series(1, %d) c""".formatted(v.colaboradores()));

		executar("""
				insert into atividade (id, tipo, data_atividade, titulo, descricao, criado_em, atualizado_em)
				select md5('atividade' || k)::uuid,
				       (array['COM_FAMILIAS','COM_USUARIOS','COM_USUARIOS_E_FAMILIAS','ATENDIMENTO_MULTIPROFISSIONAL',
				              'ATENDIMENTO_TECNICO_INDIVIDUALIZADO','ESTUDO_DE_CASO'])[1 + k %% 6],
				       %s + (k::bigint * %d / %d)::int, 'Atividade fictícia ' || k, ?, now(), now()
				from generate_series(1, %d) k""".formatted(inicio, dias, v.atividades(), v.atividades()), cifrado);
		executar("""
				insert into atividade_participante (atividade_id, pessoa_id)
				select md5('atividade' || k)::uuid, md5('pessoa' || (3 * (1 + (k * 7 + j * 13) %% %d)))::uuid
				from generate_series(1, %d) k
				cross join generate_series(0, %d) j""".formatted(inseridos, v.atividades(),
				v.participantesPorAtividade() - 1));
		executar("""
				insert into atividade_profissional (atividade_id, colaborador_id)
				select md5('atividade' || k)::uuid, md5('colaborador' || (1 + (k + j) %% %d))::uuid
				from generate_series(1, %d) k
				cross join generate_series(0, 1) j
				where j = 0 or k %% 2 = 0""".formatted(v.colaboradores(), v.atividades()));

		executar("""
				insert into comunicacao (id, tipo, direcao, data_comunicacao, contraparte, assunto, descricao,
				                         criado_em, atualizado_em)
				select md5('comunicacao' || k)::uuid, t.tipo,
				       case when t.tipo in ('OFICIO','DECLARACAO','ENCAMINHAMENTO')
				            then (array['ENVIADA','RECEBIDA'])[1 + k %% 2] end,
				       %s + (k::bigint * %d / %d)::int, 'Instituição Fictícia ' || (k %% 50), 'Assunto fictício ' || k,
				       case when k %% 3 = 0 then ? end, now(), now()
				from generate_series(1, %d) k
				cross join lateral (select (array['CONTATO_CUIDADOR_FAMILIAR','CONTATO_INSTITUCIONAL',
				        'ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO','OFICIO','DECLARACAO','ENCAMINHAMENTO'])[1 + k %% 6] as tipo) t"""
			.formatted(inicio, dias, v.comunicacoes(), v.comunicacoes()), cifrado);

		// Gestão institucional: vínculos da equipe, parcerias, articulações, eventos e doações.
		executar("""
				insert into vinculo_colaborador (id, colaborador_id, data_contratacao, data_desligamento,
				                                 motivo_desligamento, criado_em, atualizado_em)
				select md5('vinculo' || c)::uuid, md5('colaborador' || c)::uuid, %s + (c * 7 %% %d),
				       case when c %% 10 = 0 then %s end, case when c %% 10 = 0 then ? end, now(), now()
				from generate_series(1, %d) c""".formatted(inicio, dias / 2, limite, v.colaboradores()), cifrado);
		executar("""
				insert into parceiro (id, nome, tipo, vigencia_inicio, vigencia_fim, ativo, criado_em, atualizado_em)
				select md5('parceiro' || k)::uuid, 'Parceiro Fictício ' || k,
				       (array['ORGAO_PUBLICO','ORGANIZACAO_SOCIEDADE_CIVIL','EMPRESA','INSTITUICAO_ENSINO','OUTRO'])[1 + k %% 5],
				       %s + (k * 11 %% %d), case when k %% 3 = 0 then %s end, k %% 4 <> 0, now(), now()
				from generate_series(1, %d) k""".formatted(inicio, dias, limite, v.parceiros()));
		executar("""
				insert into articulacao_parceria (id, parceiro_id, data, descricao, criado_em, atualizado_em)
				select md5('articulacao' || k)::uuid, md5('parceiro' || (1 + k %% %d))::uuid,
				       %s + (k::bigint * %d / %d)::int, 'Articulação fictícia ' || k, now(), now()
				from generate_series(1, %d) k""".formatted(v.parceiros(), inicio, dias, v.articulacoes(),
				v.articulacoes()));
		executar("""
				insert into evento_institucional (id, tipo, direcao, data, descricao, parceiro_id, criado_em,
				                                  atualizado_em)
				select md5('evento' || k)::uuid, t.tipo,
				       case t.tipo when 'VISITA_INSTITUCIONAL' then 'REALIZADO' when 'EVENTO' then 'PARTICIPADO' end,
				       %s + (k::bigint * %d / %d)::int, ?,
				       case when t.tipo = 'REUNIAO_PARCEIROS' then md5('parceiro' || (1 + k %% %d))::uuid end, now(), now()
				from generate_series(1, %d) k
				cross join lateral (select (array['REUNIAO_COORDENACAO_EQUIPE','FORMACAO_MULTIPROFISSIONAL',
				        'REUNIAO_DIRECAO_GERAL','MONITORAMENTO_ORGAO_GESTOR','VISITA_INSTITUCIONAL','EVENTO',
				        'REUNIAO_PARCEIROS'])[1 + k %% 7] as tipo) t""".formatted(inicio, dias, v.eventos(),
				v.parceiros(), v.eventos()), cifrado);
		executar("""
				insert into doacao (id, direcao, data, contraparte, tipo, descricao, valor, criado_em, atualizado_em)
				select md5('doacao' || k)::uuid, case when k %% 5 = 0 then 'REALIZADA' else 'RECEBIDA' end,
				       %s + (k::bigint * %d / %d)::int, 'Doador Fictício ' || (k %% 80), t.tipo, 'Doação fictícia ' || k,
				       case when t.tipo = 'DINHEIRO' then 100 + k %% 900 end, now(), now()
				from generate_series(1, %d) k
				cross join lateral (select (array['DINHEIRO','ALIMENTOS','VESTUARIO','HIGIENE_LIMPEZA','MATERIAL_PEDAGOGICO',
				        'MOVEIS_EQUIPAMENTOS','OUTRO'])[1 + k %% 7] as tipo) t""".formatted(inicio, dias, v.doacoes(),
				v.doacoes()));

		// Log de auditoria: 20 contas, espalhado pelo período em ordem crescente.
		executar("""
				insert into auditoria (id, ocorrido_em, conta_id, conta_login, acao, entidade, entidade_id, ip)
				select md5('auditoria' || k)::uuid,
				       (%s::timestamptz + make_interval(secs => k::double precision * %d * 86400 / %d)),
				       md5('conta' || (k %% 20))::uuid, 'conta.ficticia' || (k %% 20),
				       (array['CRIAR','ALTERAR','LER_SENSIVEL','EXPORTAR','LOGIN','LOGOUT'])[1 + k %% 6],
				       (array['inscricao_lista_espera','insercao','atividade','comunicacao','violacao_direito','conta_acesso',
				              'escuta','pia'])[1 + k %% 8],
				       md5('entidade' || (k %% 5000))::uuid::text, '10.0.0.' || (k %% 250)
				from generate_series(1, %d) k""".formatted(inicio, dias, v.auditoria(), v.auditoria()));

		// Como o autovacuum faria num banco em uso: estatísticas do planejador e mapa de visibilidade, que
		// permite ao Postgres contar pelo índice sem ler a tabela.
		jdbc.execute("vacuum analyze");

		Map<String, Long> totais = new LinkedHashMap<>();
		for (String tabela : new String[] { "pessoa", "inscricao_lista_espera", "historico_situacao_inscricao",
				"insercao", "desligamento", "cadastro_complementar", "documento_pessoa", "violacao_direito",
				"colaborador", "vinculo_colaborador", "atividade", "atividade_participante", "atividade_profissional",
				"comunicacao", "parceiro", "articulacao_parceria", "evento_institucional", "doacao", "auditoria" }) {
			totais.put(tabela, jdbc.queryForObject("select count(*) from " + tabela, Long.class));
		}
		return totais;
	}

	/** O mesmo id que o SQL da massa gera com {@code md5(texto)::uuid}. */
	static String uuid(String texto) {
		try {
			String hex = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(texto.getBytes(StandardCharsets.UTF_8)));
			return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-"
					+ hex.substring(16, 20) + "-" + hex.substring(20);
		}
		catch (NoSuchAlgorithmException erro) {
			throw new IllegalStateException(erro);
		}
	}

	private void executar(String sql, Object... parametros) {
		jdbc.update(sql, parametros);
	}

}
