package br.org.cesjo.sgi.demonstracao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.org.cesjo.sgi.arquivos.Arquivos;
import br.org.cesjo.sgi.arquivos.NovoArquivo;
import br.org.cesjo.sgi.compartilhado.criptografia.Cifrador;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gera e grava a massa de demonstração (ADR 0237): cerca de dois anos de uso de um Centro-Dia, com
 * 220 pessoas cadastradas na lista de espera, 150 delas inseridas no serviço, e o que a equipe registra
 * no dia a dia. Grava direto no banco, como a massa do teste de desempenho (ADR 0230), mas com dados
 * coerentes entre si: datas em ordem, situações compatíveis com o histórico, cada texto sensível cifrado
 * em separado pelo {@link Cifrador} e os documentos guardados de verdade pelo módulo de arquivos.
 *
 * <p>
 * O sorteio tem semente fixa: a mesma data de hoje gera sempre a mesma massa.
 */
final class GeradorDemonstracao {

	static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

	static final int ATIVOS = 56;

	static final int DESLIGADOS = 94;

	static final int AGUARDANDO = 34;

	static final int EM_AVALIACAO = 6;

	static final int SAIRAM = 30;

	static final int VAGAS = 60;

	private static final long SEMENTE = 2026;

	private static final JsonMapper JSON = JsonMapper.builder().build();

	record Conta(UUID id, String login, String nome, String perfil, int ip) {
	}

	record Colaborador(UUID id, String nome, String funcao, LocalDate contratacao, LocalDate desligamento) {

		boolean ativoEm(LocalDate data) {
			return !contratacao.isAfter(data) && (desligamento == null || desligamento.isAfter(data));
		}

		boolean tecnico() {
			return !funcao.equals("OUTRA");
		}

	}

	/** Uma pessoa inserida no serviço; {@code fim} é a data do desligamento, nula quando segue ativa. */
	record Atendido(UUID pessoaId, String nome, String responsavel, LocalDate insercao, LocalDate fim) {

		boolean ativoEm(LocalDate data) {
			return !insercao.isAfter(data) && (fim == null || fim.isAfter(data));
		}

	}

	private record Registro(OffsetDateTime quando, Conta conta, String acao, String entidade, UUID entidadeId,
			String campos) {
	}

	private final JdbcTemplate jdbc;

	private final Cifrador cifrador;

	private final Arquivos arquivos;

	private final EntityManager entidades;

	private final PasswordEncoder codificador;

	private final Random sorteio = new Random(SEMENTE);

	private final Catalogo catalogo = new Catalogo(sorteio);

	private final LocalDate hoje;

	private final LocalDate inicio;

	private final List<Registro> auditoria = new ArrayList<>();

	private final List<Colaborador> colaboradores = new ArrayList<>();

	private final List<Atendido> atendidos = new ArrayList<>();

	private final List<UUID> parceiros = new ArrayList<>();

	private final List<UUID> candidatosNaFila = new ArrayList<>();

	private Conta coordenador;

	private Conta tecnica;

	private Conta administrativo;

	private Conta consulta;

	GeradorDemonstracao(JdbcTemplate jdbc, Cifrador cifrador, Arquivos arquivos, EntityManager entidades,
			PasswordEncoder codificador, LocalDate hoje) {
		this.jdbc = jdbc;
		this.cifrador = cifrador;
		this.arquivos = arquivos;
		this.entidades = entidades;
		this.codificador = codificador;
		this.hoje = hoje;
		this.inicio = hoje.minusYears(2);
	}

	/** Grava a massa inteira e devolve o total de linhas das principais tabelas. */
	Map<String, Long> gravar(String senha) {
		gravarContas(senha);
		gravarColaboradores();
		gravarCapacidade();
		gravarParceiros();
		gravarPessoas();
		gravarAtividades();
		gravarComunicacoes();
		gravarEventos();
		gravarDoacoes();
		gravarNotificacoes();
		gravarAcessos();
		gravarAuditoria();

		Map<String, Long> totais = new LinkedHashMap<>();
		for (String tabela : new String[] { "conta_acesso", "colaborador", "pessoa", "inscricao_lista_espera",
				"insercao", "desligamento", "visita_domiciliar", "escuta", "pia", "violacao_direito", "atividade",
				"comunicacao", "parceiro", "evento_institucional", "doacao", "arquivo", "auditoria" }) {
			totais.put(tabela, jdbc.queryForObject("select count(*) from " + tabela, Long.class));
		}
		return totais;
	}

	// ---------------------------------------------------------------- equipe e contas

	private void gravarContas(String senha) {
		String hash = codificador.encode(senha);
		coordenador = conta("coordenador", "Cláudia Ribeiro Moreira", "COORDENADOR", 11, hash);
		tecnica = conta("equipe.tecnica", "Renata Duarte Campos", "EQUIPE_TECNICA", 12, hash);
		administrativo = conta("administrativo", "Paulo Henrique Lopes", "ADMINISTRATIVO", 13, hash);
		consulta = conta("consulta", "Marcos Teixeira Vieira", "CONSULTA", 14, hash);
	}

	private Conta conta(String login, String nome, String perfil, int ip, String hash) {
		Conta conta = new Conta(id(), login, nome, perfil, ip);
		OffsetDateTime criada = momento(inicio.minusDays(10));
		jdbc.update("""
				insert into conta_acesso (id, login, nome, senha_hash, perfil, ativa, troca_senha_obrigatoria,
				                          criado_em, criado_por, atualizado_em)
				values (?, ?, ?, ?, ?, true, false, ?, ?, ?)""", conta.id(), login, nome, hash, perfil, criada,
				coordenador == null ? null : coordenador.id(), criada);
		auditar(criada, coordenador == null ? conta : coordenador, "CRIAR", "conta_acesso", conta.id());
		return conta;
	}

	private void gravarColaboradores() {
		LocalDate antes = inicio.minusMonths(8);
		colaborador("Cláudia Ribeiro Moreira", "OUTRA", antes, null, coordenador);
		colaborador("Renata Duarte Campos", "ASSISTENTE_SOCIAL", antes, null, tecnica);
		colaborador("Paulo Henrique Lopes", "OUTRA", antes.plusMonths(2), null, administrativo);
		colaborador("Juliana Freitas Almeida", "ASSISTENTE_SOCIAL", antes.plusMonths(1), null, null);
		colaborador("Fernanda Costa Machado", "PSICOLOGO", antes, null, null);
		colaborador("Rafael Gomes Barbosa", "PSICOLOGO", inicio.plusMonths(9), null, null);
		colaborador("Larissa Mendes Rocha", "TERAPEUTA_OCUPACIONAL", antes, null, null);
		colaborador("Thiago Cardoso Lima", "TERAPEUTA_OCUPACIONAL", inicio.plusMonths(14), null, null);
		colaborador("Aline Batista Reis", "OUTRA", antes, null, null);
		colaborador("Márcio Pinto Fonseca", "OUTRA", antes.plusMonths(3), null, null);
		colaborador("Sônia Araújo Dias", "OUTRA", antes, null, null);
		colaborador("Priscila Monteiro Santos", "OUTRA", inicio.plusMonths(5), null, null);
		colaborador("Débora Vieira Oliveira", "ASSISTENTE_SOCIAL", antes, inicio.plusMonths(7), null);
		colaborador("Gustavo Martins Pereira", "PSICOLOGO", antes, inicio.plusMonths(8), null);
		colaborador("Elaine Souza Carvalho", "OUTRA", antes, inicio.plusMonths(16), null);
	}

	private void colaborador(String nome, String funcao, LocalDate contratacao, LocalDate desligamento,
			Conta conta) {
		Colaborador colaborador = new Colaborador(id(), nome, funcao, contratacao, desligamento);
		colaboradores.add(colaborador);
		OffsetDateTime criado = momento(inicio.minusDays(5));
		jdbc.update("""
				insert into colaborador (id, nome, funcao, ativo, conta_acesso_id, criado_em, criado_por,
				                         atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?)""", colaborador.id(), nome, funcao, desligamento == null,
				conta == null ? null : conta.id(), criado, coordenador.id(), criado);
		auditar(criado, coordenador, "CRIAR", "colaborador", colaborador.id());
		UUID vinculo = id();
		jdbc.update("""
				insert into vinculo_colaborador (id, colaborador_id, data_contratacao, data_desligamento,
				                                 motivo_desligamento, criado_em, criado_por, atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?)""", vinculo, colaborador.id(), contratacao, desligamento,
				desligamento == null ? null : cifrar("Pedido de desligamento para assumir outro trabalho."), criado,
				coordenador.id(), criado);
		auditar(criado, coordenador, "CRIAR", "vinculo_colaborador", vinculo);
	}

	private void gravarCapacidade() {
		jdbc.update("insert into capacidade_servico (id, vagas, definida_por, definida_em) values (?, ?, ?, ?)",
				id(), 50, coordenador.id(), momento(inicio));
		OffsetDateTime ampliacao = momento(inicio.plusYears(1));
		UUID id = id();
		jdbc.update("insert into capacidade_servico (id, vagas, definida_por, definida_em) values (?, ?, ?, ?)", id,
				VAGAS, coordenador.id(), ampliacao);
		auditar(ampliacao, coordenador, "CRIAR", "capacidade_servico", id);
	}

	// ---------------------------------------------------------------- parcerias

	private void gravarParceiros() {
		int k = 0;
		for (Catalogo.Parceiro modelo : Catalogo.PARCEIROS) {
			UUID id = id();
			boolean encerrado = k == 4 || k == 10;
			LocalDate inicioVigencia = modelo.instrumento() == null ? null : inicio.minusMonths(sorteio.nextInt(18));
			LocalDate fimVigencia = inicioVigencia == null ? null
					: encerrado ? inicio.plusMonths(10) : inicioVigencia.plusYears(3);
			OffsetDateTime criado = momento(inicio.plusDays(sorteio.nextInt(20)));
			jdbc.update("""
					insert into parceiro (id, nome, tipo, instrumento, vigencia_inicio, vigencia_fim, contato,
					                      observacoes, ativo, criado_em, criado_por, atualizado_em)
					values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", id, modelo.nome(), modelo.tipo(),
					modelo.instrumento(), inicioVigencia, fimVigencia,
					catalogo.nome(sorteio.nextBoolean(), catalogo.sobrenomes()) + " — " + formatarTelefone(catalogo.telefone()),
					k % 3 == 0 ? "Parceria acompanhada pela coordenação em reuniões trimestrais." : null, !encerrado,
					criado, coordenador.id(), criado);
			auditar(criado, coordenador, "CRIAR", "parceiro", id);
			parceiros.add(id);
			int articulacoes = encerrado ? 2 : 3 + sorteio.nextInt(5);
			for (int a = 0; a < articulacoes; a++) {
				LocalDate data = diaUtil(entre(inicio, encerrado ? inicio.plusMonths(10) : hoje.minusDays(1)));
				UUID articulacao = id();
				OffsetDateTime registro = momento(data);
				jdbc.update("""
						insert into articulacao_parceria (id, parceiro_id, data, descricao, criado_em, criado_por,
						                                  atualizado_em)
						values (?, ?, ?, ?, ?, ?, ?)""", articulacao, id, data, catalogo.um(Catalogo.ARTICULACOES),
						registro, coordenador.id(), registro);
				auditar(registro, coordenador, "CRIAR", "articulacao_parceria", articulacao);
			}
			k++;
		}
	}

	// ---------------------------------------------------------------- pessoas, fila e atendimento

	private void gravarPessoas() {
		for (int i = 0; i < DESLIGADOS; i++) {
			LocalDate insercao = entre(inicio, hoje.minusDays(90));
			LocalDate fim = minimo(insercao.plusDays(60 + sorteio.nextInt(300)), hoje.minusDays(1));
			pessoaInserida(diaUtil(insercao), diaUtil(fim));
		}
		for (int i = 0; i < ATIVOS; i++) {
			pessoaInserida(diaUtil(entre(inicio, hoje.minusDays(15))), null);
		}
		for (int i = 0; i < SAIRAM; i++) {
			pessoaQueSaiu();
		}
		for (int i = 0; i < AGUARDANDO; i++) {
			candidato(diaUtil(entre(hoje.minusDays(420), hoje.minusDays(1))), false);
		}
		for (int i = 0; i < EM_AVALIACAO; i++) {
			candidato(diaUtil(entre(hoje.minusDays(200), hoje.minusDays(20))), true);
		}
	}

	private record Pessoa(UUID id, UUID responsavel, String nome, String nomeResponsavel) {
	}

	private Pessoa pessoa(LocalDate cadastro, int idadeMinima, int idadeMaxima) {
		boolean feminina = sorteio.nextInt(100) < 45;
		String sobrenomes = catalogo.sobrenomes();
		String nome = catalogo.nome(feminina, sobrenomes);
		LocalDate nascimento = hoje.minusYears(idadeMinima + sorteio.nextInt(idadeMaxima - idadeMinima + 1))
			.minusDays(sorteio.nextInt(365));
		UUID id = id();
		OffsetDateTime criado = momento(cadastro);
		jdbc.update("""
				insert into pessoa (id, nome_completo, data_nascimento, tipo_deficiencia, telefone, criado_em,
				                    criado_por, atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?)""", id, nome, nascimento, tipoDeficiencia(), catalogo.telefone(),
				criado, administrativo.id(), criado);
		auditar(criado, administrativo, "CRIAR", "pessoa", id);
		String sobrenomeResponsavel = catalogo.um(Catalogo.SOBRENOMES) + " " + sobrenomes.split(" ")[1];
		String nomeResponsavel = catalogo.nome(sorteio.nextInt(100) < 80, sobrenomeResponsavel);
		UUID responsavel = id();
		jdbc.update("""
				insert into responsavel (id, pessoa_id, nome_completo, criado_em, criado_por, atualizado_em)
				values (?, ?, ?, ?, ?, ?)""", responsavel, id, nomeResponsavel, criado, administrativo.id(), criado);
		return new Pessoa(id, responsavel, nome, nomeResponsavel);
	}

	private UUID inscricao(Pessoa pessoa, LocalDate cadastro, String situacao, LocalDate saida, String motivo) {
		UUID id = id();
		OffsetDateTime criado = momento(cadastro);
		jdbc.update("""
				insert into inscricao_lista_espera (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao,
				                                    data_saida, motivo_saida, criado_em, criado_por, atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", id, pessoa.id(), cadastro, origem(), situacao, saida, motivo,
				criado, administrativo.id(), criado);
		auditar(criado, administrativo, "CRIAR", "inscricao_lista_espera", id);
		historico(id, null, "AGUARDANDO", cadastro, null, null, administrativo);
		return id;
	}

	private void historico(UUID inscricao, String anterior, String nova, LocalDate data, String motivo,
			String observacao, Conta conta) {
		jdbc.update("""
				insert into historico_situacao_inscricao (id, inscricao_id, situacao_anterior, situacao_nova, data,
				                                          motivo_saida, observacao, registrado_por, registrado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?)""", id(), inscricao, anterior, nova, data, motivo,
				observacao == null ? null : cifrar(observacao), conta.id(), momento(data));
		if (anterior != null) {
			auditar(momento(data), conta, "ALTERAR", "inscricao_lista_espera", inscricao, "situacao");
		}
	}

	private void pessoaInserida(LocalDate insercao, LocalDate fim) {
		LocalDate cadastro = diaUtil(insercao.minusDays(25 + sorteio.nextInt(130)));
		Pessoa pessoa = pessoa(cadastro, 18, 58);
		UUID inscricao = inscricao(pessoa, cadastro, "INSERIDO", null, null);

		// Avaliação: visita agendada alguns dias depois do cadastro e realizada antes da inserção.
		long folga = ChronoUnit.DAYS.between(cadastro, insercao);
		LocalDate agendamento = cadastro.plusDays(1 + sorteio.nextInt((int) Math.max(1, folga / 3)));
		LocalDate avaliacao = diaUtil(agendamento.plusDays(3 + sorteio.nextInt(7)));
		if (!avaliacao.isBefore(insercao)) {
			avaliacao = insercao.minusDays(2);
		}
		historico(inscricao, "AGUARDANDO", "EM_AVALIACAO", agendamento, null, null, tecnica);
		visita(pessoa.id(), avaliacao, "REALIZADA", agendamento, null);
		historico(inscricao, "EM_AVALIACAO", "INSERIDO", insercao, null, null, coordenador);

		UUID insercaoId = id();
		OffsetDateTime inserido = momento(insercao);
		jdbc.update("""
				insert into insercao (id, pessoa_id, inscricao_id, data_insercao, turno, cuidador_responsavel_id,
				                      situacao, criado_em, criado_por, atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", insercaoId, pessoa.id(), inscricao, insercao,
				catalogo.um(List.of("MATUTINO", "MATUTINO", "VESPERTINO", "VESPERTINO", "INTEGRAL")),
				pessoa.responsavel(), fim == null ? "ATIVA" : "DESLIGADA", inserido, coordenador.id(), inserido);
		auditar(inserido, coordenador, "CRIAR", "insercao", insercaoId);

		Atendido atendido = new Atendido(pessoa.id(), pessoa.nome(), pessoa.nomeResponsavel(), insercao, fim);
		atendidos.add(atendido);
		cadastroComplementar(pessoa.id(), insercao);
		if (fim == null) {
			documentos(pessoa.id(), cadastro);
		}
		LocalDate ultimoDia = fim == null ? hoje : fim;
		escutas(atendido, ultimoDia);
		pias(atendido, ultimoDia);
		violacoes(atendido, ultimoDia);
		acompanhamentos(atendido, ultimoDia);
		if (fim != null) {
			desligamento(insercaoId, fim);
		}
	}

	private void pessoaQueSaiu() {
		LocalDate cadastro = diaUtil(entre(inicio.minusMonths(2), hoje.minusDays(60)));
		LocalDate saida = diaUtil(minimo(cadastro.plusDays(15 + sorteio.nextInt(110)), hoje.minusDays(1)));
		String motivo = catalogo.um(List.of("DESISTENCIA", "DESISTENCIA", "NAO_LOCALIZADO", "FORA_DO_PERFIL", "OUTRO"));
		Pessoa pessoa = pessoa(cadastro, 17, 62);
		UUID inscricao = inscricao(pessoa, cadastro, "SAIU", saida, motivo);
		String observacao = switch (motivo) {
			case "DESISTENCIA" -> Catalogo.OBSERVACOES_SAIDA.get(0);
			case "NAO_LOCALIZADO" -> Catalogo.OBSERVACOES_SAIDA.get(1);
			case "FORA_DO_PERFIL" -> Catalogo.OBSERVACOES_SAIDA.get(2);
			default -> Catalogo.OBSERVACOES_SAIDA.get(3);
		};
		historico(inscricao, "AGUARDANDO", "SAIU", saida, motivo, observacao, administrativo);
	}

	private void candidato(LocalDate cadastro, boolean emAvaliacao) {
		Pessoa pessoa = pessoa(cadastro, 17, 60);
		UUID inscricao = inscricao(pessoa, cadastro, emAvaliacao ? "EM_AVALIACAO" : "AGUARDANDO", null, null);
		candidatosNaFila.add(inscricao);
		if (sorteio.nextInt(100) < 70) {
			documentos(pessoa.id(), cadastro);
		}
		if (!emAvaliacao) {
			return;
		}
		LocalDate agendamento = diaUtil(hoje.minusDays(2 + sorteio.nextInt(10)));
		LocalDate cancelamento = minimo(agendamento.plusDays(1), hoje.minusDays(1));
		historico(inscricao, "AGUARDANDO", "EM_AVALIACAO", agendamento, null, null, administrativo);
		LocalDate data = diaUtil(hoje.plusDays(1 + sorteio.nextInt(15)));
		if (candidatosNaFila.size() % 3 == 0) {
			// Uma visita cancelada antes, reagendada para a data nova.
			UUID cancelada = visita(pessoa.id(), cancelamento, "CANCELADA", agendamento, null);
			visita(pessoa.id(), data, "AGENDADA", cancelamento, cancelada);
		}
		else {
			visita(pessoa.id(), data, "AGENDADA", agendamento, null);
		}
	}

	private UUID visita(UUID pessoa, LocalDate data, String status, LocalDate agendadaEm, UUID origem) {
		UUID id = id();
		LocalDateTime dataHora = data.atTime(sorteio.nextBoolean() ? 9 : 14, sorteio.nextBoolean() ? 0 : 30);
		OffsetDateTime criado = momento(agendadaEm);
		Conta autor = sorteio.nextBoolean() ? administrativo : tecnica;
		jdbc.update("""
				insert into visita_domiciliar (id, pessoa_id, data_hora, status, motivo_cancelamento, observacoes,
				                               visita_origem_id, criado_em, criado_por, atualizado_em, atualizado_por)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", id, pessoa, dataHora, status,
				status.equals("CANCELADA") ? catalogo.um(Catalogo.MOTIVOS_CANCELAMENTO) : null,
				status.equals("REALIZADA") ? cifrar(catalogo.um(Catalogo.OBSERVACOES_VISITA)) : null, origem, criado,
				autor.id(), status.equals("AGENDADA") ? criado : momento(data), autor.id());
		auditar(criado, autor, "CRIAR", "visita_domiciliar", id);
		if (!status.equals("AGENDADA")) {
			auditar(momento(data), status.equals("REALIZADA") ? tecnica : autor, "ALTERAR", "visita_domiciliar", id,
					"status");
		}
		for (Colaborador profissional : tecnicos(data, 1 + sorteio.nextInt(2))) {
			jdbc.update("insert into visita_profissional (visita_id, colaborador_id) values (?, ?)", id,
					profissional.id());
		}
		return id;
	}

	private void acompanhamentos(Atendido atendido, LocalDate ultimoDia) {
		int visitas = sorteio.nextInt(3);
		for (int v = 0; v < visitas; v++) {
			LocalDate data = diaUtil(entre(atendido.insercao().plusDays(30), ultimoDia.minusDays(1)));
			if (data.isBefore(ultimoDia) && data.isAfter(atendido.insercao())) {
				visita(atendido.pessoaId(), data, "REALIZADA", data.minusDays(7), null);
			}
		}
		if (atendido.fim() == null && sorteio.nextInt(100) < 15) {
			visita(atendido.pessoaId(), diaUtil(hoje.plusDays(2 + sorteio.nextInt(25))), "AGENDADA", hoje.minusDays(2),
					null);
		}
	}

	private void cadastroComplementar(UUID pessoa, LocalDate insercao) {
		OffsetDateTime criado = momento(insercao);
		boolean bpc = sorteio.nextInt(100) < 55;
		jdbc.update("""
				insert into cadastro_complementar (pessoa_id, genero, cpf, nis, rg, escolaridade, cep, logradouro,
				                                   numero, complemento, bairro, cidade, renda_familiar, recebe_bpc,
				                                   inscrito_cad_unico, recebe_bolsa_familia, crenca_religiosa,
				                                   medicamentos_uso_continuo, observacoes_saude, criado_em, criado_por,
				                                   atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", pessoa,
				catalogo.um(List.of("FEMININO", "MASCULINO", "MASCULINO", "FEMININO", "OUTRO")),
				cifrar(catalogo.cpf()), cifrar(catalogo.nis()), cifrar(catalogo.rg()),
				catalogo.um(List.of("SEM_ESCOLARIDADE", "FUNDAMENTAL_INCOMPLETO", "FUNDAMENTAL_INCOMPLETO",
						"FUNDAMENTAL_COMPLETO", "MEDIO_INCOMPLETO", "MEDIO_COMPLETO", "SUPERIOR_INCOMPLETO")),
				catalogo.cep(), catalogo.um(Catalogo.LOGRADOUROS), String.valueOf(10 + sorteio.nextInt(1500)),
				sorteio.nextInt(100) < 25 ? "Casa " + (1 + sorteio.nextInt(4)) : null, catalogo.um(Catalogo.BAIRROS),
				Catalogo.CIDADE,
				BigDecimal.valueOf(600 + sorteio.nextInt(4200) + sorteio.nextInt(100) / 100.0).setScale(2,
						RoundingMode.HALF_UP),
				bpc, sorteio.nextInt(100) < 75, !bpc && sorteio.nextInt(100) < 40,
				catalogo.um(List.of("CATOLICA", "CATOLICA", "EVANGELICA", "EVANGELICA", "MATRIZ_AFRICANA",
						"TESTEMUNHA_DE_JEOVA", "OUTRAS")),
				sorteio.nextInt(100) < 65 ? cifrar(catalogo.um(Catalogo.MEDICAMENTOS)) : null,
				cifrar(catalogo.um(Catalogo.OBSERVACOES_SAUDE)), criado, tecnica.id(), criado);
		auditar(criado, tecnica, "CRIAR", "cadastro_complementar", pessoa);
	}

	private void documentos(UUID pessoa, LocalDate cadastro) {
		OffsetDateTime enviado = momento(cadastro);
		for (String tipo : List.of("LAUDO_MEDICO", "COMPROVANTE_RESIDENCIA")) {
			String titulo = tipo.equals("LAUDO_MEDICO") ? "Laudo médico" : "Comprovante de residência";
			UUID arquivo = guardar(tipo.equals("LAUDO_MEDICO") ? "laudo-medico.pdf" : "comprovante-residencia.pdf",
					titulo + " (documento fictício de demonstração)");
			jdbc.update("update arquivo set enviado_por = ?, enviado_em = ? where id = ?", administrativo.id(), enviado,
					arquivo);
			UUID documento = id();
			jdbc.update("""
					insert into documento_pessoa (id, pessoa_id, tipo, arquivo_id, criado_em, criado_por, atualizado_em)
					values (?, ?, ?, ?, ?, ?, ?)""", documento, pessoa, tipo, arquivo, enviado, administrativo.id(),
					enviado);
		}
	}

	private void escutas(Atendido atendido, LocalDate ultimoDia) {
		LocalDate data = diaUtil(minimo(atendido.insercao().plusDays(3 + sorteio.nextInt(15)), ultimoDia));
		escuta(atendido.pessoaId(), data);
		LocalDate segunda = data.plusMonths(6);
		if (sorteio.nextInt(100) < 35 && segunda.isBefore(ultimoDia)) {
			escuta(atendido.pessoaId(), diaUtil(segunda));
		}
	}

	private void escuta(UUID pessoa, LocalDate data) {
		UUID id = id();
		OffsetDateTime criado = momento(data);
		jdbc.update("""
				insert into escuta (id, pessoa_id, data_escuta, colaborador_id, dinamica_familiar,
				                    necessidades_imediatas, habilidades, potencialidades, criado_em, criado_por,
				                    atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", id, pessoa, data, tecnicos(data, 1).getFirst().id(),
				cifrar(catalogo.um(Catalogo.DINAMICAS_FAMILIARES)), cifrar(catalogo.um(Catalogo.NECESSIDADES)),
				cifrar(catalogo.um(Catalogo.HABILIDADES)), cifrar(catalogo.um(Catalogo.POTENCIALIDADES)), criado,
				tecnica.id(), criado);
		auditar(criado, tecnica, "CRIAR", "escuta", id);
	}

	private void pias(Atendido atendido, LocalDate ultimoDia) {
		LocalDate elaboracao = diaUtil(atendido.insercao().plusDays(20 + sorteio.nextInt(20)));
		if (!elaboracao.isBefore(ultimoDia)) {
			return;
		}
		Pia primeira = pia(atendido, 1, null, elaboracao, ultimoDia);
		if (atendido.fim() != null || primeira.aprovado() == null) {
			return;
		}
		LocalDate revisao = diaUtil(primeira.aprovado().plusDays(180 + sorteio.nextInt(60)));
		if (revisao.isBefore(hoje)) {
			pia(atendido, 2, primeira.id(), revisao, hoje);
		}
	}

	private record Pia(UUID id, LocalDate aprovado) {
	}

	private Pia pia(Atendido atendido, int numero, UUID anterior, LocalDate elaboracao, LocalDate limite) {
		LocalDate familia = diaUtil(elaboracao.plusDays(5 + sorteio.nextInt(10)));
		LocalDate equipe = diaUtil(familia.plusDays(3 + sorteio.nextInt(7)));
		LocalDate aprovacao = diaUtil(equipe.plusDays(2 + sorteio.nextInt(5)));
		String status = !familia.isBefore(limite) ? "ELABORADO"
				: !equipe.isBefore(limite) ? "APRESENTADO_FAMILIA"
						: !aprovacao.isBefore(limite) ? "APRESENTADO_EQUIPE" : "APROVADO";
		boolean aprovado = status.equals("APROVADO");
		UUID id = id();
		OffsetDateTime criado = momento(elaboracao);
		LocalDate ultimaMudanca = aprovado ? aprovacao
				: status.equals("APRESENTADO_EQUIPE") ? equipe : status.equals("APRESENTADO_FAMILIA") ? familia : elaboracao;
		jdbc.update("""
				insert into pia (id, pessoa_id, numero_versao, status, conteudo, versao_anterior_id,
				                 apresentado_familia_em, apresentado_equipe_em, aprovado_em, aprovado_por, criado_em,
				                 criado_por, atualizado_em, atualizado_por)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", id, atendido.pessoaId(), numero, status,
				cifrar(conteudoPia(atendido, elaboracao)), anterior, status.equals("ELABORADO") ? null : familia,
				status.equals("APRESENTADO_EQUIPE") || aprovado ? equipe : null, aprovado ? aprovacao : null,
				aprovado ? coordenador.id() : null, criado, tecnica.id(), momento(ultimaMudanca),
				aprovado ? coordenador.id() : tecnica.id());
		auditar(criado, tecnica, "CRIAR", "pia", id);
		if (!status.equals("ELABORADO")) {
			auditar(momento(ultimaMudanca), aprovado ? coordenador : tecnica, "ALTERAR", "pia", id, "status");
		}
		return new Pia(id, aprovado ? aprovacao : null);
	}

	private String conteudoPia(Atendido atendido, LocalDate elaboracao) {
		List<Map<String, Object>> objetivos = new ArrayList<>();
		Set<String> escolhidos = new LinkedHashSet<>();
		while (escolhidos.size() < 2 + sorteio.nextInt(2)) {
			escolhidos.add(catalogo.um(Catalogo.OBJETIVOS_PIA));
		}
		for (String descricao : escolhidos) {
			Map<String, Object> objetivo = new LinkedHashMap<>();
			objetivo.put("descricao", descricao);
			objetivo.put("metas", "Avançar de forma observável até a próxima revisão do plano.");
			objetivo.put("acoes", catalogo.um(Catalogo.ACOES_PIA));
			objetivo.put("prazo", elaboracao.plusMonths(6).toString());
			objetivo.put("responsavel", catalogo.um(List.of("Equipe técnica", "Família", "Serviço social",
					"Psicologia", "Terapia ocupacional", "CRAS de referência")));
			objetivos.add(objetivo);
		}
		Map<String, Object> conteudo = new LinkedHashMap<>();
		conteudo.put("diagnostico", catalogo.um(Catalogo.DINAMICAS_FAMILIARES) + " "
				+ catalogo.um(Catalogo.NECESSIDADES));
		conteudo.put("objetivos", objetivos);
		conteudo.put("observacoes", "Plano construído com " + atendido.responsavel() + ", cuidador(a) de referência.");
		return JSON.writeValueAsString(conteudo);
	}

	private void violacoes(Atendido atendido, LocalDate ultimoDia) {
		if (sorteio.nextInt(100) >= 35) {
			return;
		}
		int quantas = 1 + (sorteio.nextInt(100) < 20 ? 1 : 0);
		for (int v = 0; v < quantas; v++) {
			LocalDate data = diaUtil(entre(atendido.insercao(), ultimoDia.minusDays(1)));
			String tipo = catalogo.um(List.of("ISOLAMENTO_SOCIAL", "ISOLAMENTO_SOCIAL", "ESTRESSE_CUIDADOR",
					"ESTRESSE_CUIDADOR", "FALTA_CUIDADOS_CUIDADOR", "NEGLIGENCIA_ABANDONO", "DESVALORIZACAO_CAPACIDADE",
					"DISCRIMINACAO_FAMILIAR", "VIOLENCIA_PATRIMONIAL", "CONFINAMENTO", "VIOLENCIA_FISICA_PSICOLOGICA",
					"EXPLORACAO_IMAGEM", "ABUSO_SEXUAL", "OUTRAS"));
			String ambito = tipo.equals("ABUSO_SEXUAL") || tipo.equals("VIOLENCIA_FISICA_PSICOLOGICA")
					? catalogo.um(List.of("INTRAFAMILIAR", "INTRAFAMILIAR", "EXTRAFAMILIAR"))
					: null;
			UUID id = id();
			OffsetDateTime criado = momento(data);
			jdbc.update("""
					insert into violacao_direito (id, pessoa_id, tipo, ambito, data_identificacao, descricao, criado_em,
					                              criado_por, atualizado_em)
					values (?, ?, ?, ?, ?, ?, ?, ?, ?)""", id, atendido.pessoaId(), tipo, ambito, data,
					cifrar(catalogo.um(Catalogo.DESCRICOES_VIOLACAO)), criado, tecnica.id(), criado);
			auditar(criado, tecnica, "CRIAR", "violacao_direito", id);
		}
	}

	private void desligamento(UUID insercao, LocalDate data) {
		String motivo = catalogo.um(List.of("EVASAO", "MUDANCA_MUNICIPIO", "SOLICITACAO_FAMILIA",
				"SOLICITACAO_FAMILIA", "ALCANCE_OBJETIVOS", "ALCANCE_OBJETIVOS", "OUTROS"));
		String observacao = switch (motivo) {
			case "MUDANCA_MUNICIPIO" -> Catalogo.OBSERVACOES_DESLIGAMENTO.get(0);
			case "ALCANCE_OBJETIVOS" -> Catalogo.OBSERVACOES_DESLIGAMENTO.get(1);
			case "EVASAO" -> Catalogo.OBSERVACOES_DESLIGAMENTO.get(2);
			case "SOLICITACAO_FAMILIA" -> Catalogo.OBSERVACOES_DESLIGAMENTO.get(3);
			default -> Catalogo.OBSERVACOES_DESLIGAMENTO.get(4);
		};
		UUID id = id();
		OffsetDateTime criado = momento(data);
		jdbc.update("""
				insert into desligamento (id, insercao_id, data, motivo, observacao, criado_em, criado_por,
				                          atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?)""", id, insercao, data, motivo, cifrar(observacao), criado,
				coordenador.id(), criado);
		jdbc.update("update insercao set atualizado_em = ?, atualizado_por = ? where id = ?", criado, coordenador.id(),
				insercao);
		auditar(criado, coordenador, "CRIAR", "desligamento", id);
	}

	// ---------------------------------------------------------------- atividades e comunicações

	private void gravarAtividades() {
		for (LocalDate semana = inicio.with(DayOfWeek.MONDAY).plusWeeks(1); semana.isBefore(hoje); semana = semana
			.plusWeeks(1)) {
			atividade("COM_USUARIOS", semana.plusDays(1), catalogo.um(Catalogo.ATIVIDADES_GRUPO), 5, 12);
			if (sorteio.nextBoolean()) {
				atividade("COM_USUARIOS", semana.plusDays(3), catalogo.um(Catalogo.ATIVIDADES_GRUPO), 5, 12);
			}
			if (semana.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) % 2 == 0) {
				atividade("COM_FAMILIAS", semana.plusDays(2), catalogo.um(Catalogo.ATIVIDADES_FAMILIA), 4, 10);
				atividade("ATENDIMENTO_MULTIPROFISSIONAL", semana.plusDays(sorteio.nextInt(5)), "Atendimento multiprofissional",
						1, 1);
			}
			atividade("ATENDIMENTO_TECNICO_INDIVIDUALIZADO", semana.plusDays(sorteio.nextInt(5)),
					"Atendimento técnico individualizado", 1, 1);
			if (semana.getDayOfMonth() <= 7) {
				atividade("ESTUDO_DE_CASO", semana.plusDays(4), "Estudo de caso", 1, 1);
				if (semana.getMonthValue() == 6 || semana.getMonthValue() == 12) {
					atividade("COM_USUARIOS_E_FAMILIAS", semana.plusDays(4),
							semana.getMonthValue() == 6 ? "Festa junina" : "Confraternização de fim de ano", 15, 30);
				}
				else if (semana.getMonthValue() % 3 == 0) {
					atividade("COM_USUARIOS_E_FAMILIAS", semana.plusDays(4), catalogo.um(Catalogo.ATIVIDADES_MISTAS), 10,
							20);
				}
			}
		}
	}

	private void atividade(String tipo, LocalDate data, String titulo, int minimo, int maximo) {
		if (!data.isBefore(hoje)) {
			return;
		}
		List<Atendido> presentes = new ArrayList<>(atendidos.stream().filter(a -> a.ativoEm(data)).toList());
		if (presentes.isEmpty()) {
			return;
		}
		Collections.shuffle(presentes, sorteio);
		int quantos = Math.min(presentes.size(), minimo + sorteio.nextInt(maximo - minimo + 1));
		List<Atendido> participantes = presentes.subList(0, quantos);
		String descricao = switch (tipo) {
			case "ATENDIMENTO_MULTIPROFISSIONAL", "ATENDIMENTO_TECNICO_INDIVIDUALIZADO" ->
				catalogo.um(Catalogo.DESCRICOES_ATENDIMENTO);
			case "ESTUDO_DE_CASO" -> catalogo.um(Catalogo.DESCRICOES_ESTUDO_CASO);
			default -> catalogo.um(Catalogo.DESCRICOES_ATIVIDADE);
		};
		if (quantos == 1) {
			titulo = titulo + " — " + participantes.getFirst().nome();
		}
		UUID id = id();
		OffsetDateTime criado = momento(data);
		jdbc.update("""
				insert into atividade (id, tipo, data_atividade, titulo, descricao, criado_em, criado_por,
				                       atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?)""", id, tipo, data, titulo, cifrar(descricao), criado, tecnica.id(),
				criado);
		auditar(criado, tecnica, "CRIAR", "atividade", id);
		for (Atendido participante : participantes) {
			jdbc.update("insert into atividade_participante (atividade_id, pessoa_id) values (?, ?)", id,
					participante.pessoaId());
		}
		int profissionais = tipo.equals("ATENDIMENTO_MULTIPROFISSIONAL") || tipo.equals("ESTUDO_DE_CASO") ? 3
				: 1 + sorteio.nextInt(2);
		for (Colaborador profissional : tecnicos(data, profissionais)) {
			jdbc.update("insert into atividade_profissional (atividade_id, colaborador_id) values (?, ?)", id,
					profissional.id());
		}
	}

	private void gravarComunicacoes() {
		for (LocalDate dia = inicio.plusDays(3); dia.isBefore(hoje); dia = dia.plusDays(1 + sorteio.nextInt(5))) {
			LocalDate data = diaUtil(dia);
			if (!data.isBefore(hoje)) {
				break;
			}
			List<Atendido> ativos = atendidos.stream().filter(a -> a.ativoEm(data)).toList();
			int sorte = sorteio.nextInt(100);
			String tipo = sorte < 40 ? "CONTATO_CUIDADOR_FAMILIAR"
					: sorte < 60 ? "CONTATO_INSTITUCIONAL"
							: sorte < 70 ? "ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO"
									: sorte < 85 ? "OFICIO" : sorte < 93 ? "DECLARACAO" : "ENCAMINHAMENTO";
			if (ativos.isEmpty() && (tipo.equals("CONTATO_CUIDADOR_FAMILIAR") || tipo.equals("DECLARACAO"))) {
				tipo = "CONTATO_INSTITUCIONAL";
			}
			Atendido atendido = ativos.isEmpty() ? null : catalogo.um(ativos);
			String contraparte;
			String assunto;
			String direcao = null;
			switch (tipo) {
				case "CONTATO_CUIDADOR_FAMILIAR" -> {
					contraparte = atendido.responsavel();
					assunto = catalogo.um(Catalogo.ASSUNTOS_CONTATO);
				}
				case "ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO" -> {
					contraparte = catalogo.nome(sorteio.nextBoolean(), catalogo.sobrenomes());
					assunto = "Informações sobre o serviço e a lista de espera";
				}
				case "DECLARACAO" -> {
					contraparte = atendido.responsavel();
					assunto = "Declaração de frequência de " + atendido.nome();
					direcao = "ENVIADA";
				}
				case "OFICIO", "ENCAMINHAMENTO" -> {
					contraparte = catalogo.um(Catalogo.INSTITUICOES);
					assunto = tipo.equals("OFICIO") ? catalogo.um(Catalogo.ASSUNTOS_OFICIO)
							: "Encaminhamento para atendimento na rede";
					direcao = sorteio.nextInt(100) < 60 ? "ENVIADA" : "RECEBIDA";
				}
				default -> {
					contraparte = catalogo.um(Catalogo.INSTITUICOES);
					assunto = catalogo.um(Catalogo.ASSUNTOS_CONTATO);
				}
			}
			UUID arquivo = null;
			if (tipo.equals("OFICIO") && sorteio.nextInt(100) < 40) {
				arquivo = guardar("oficio.pdf", "Ofício: " + assunto + " (fictício)");
			}
			Conta autor = tipo.startsWith("CONTATO") || tipo.startsWith("ATENDIMENTO") ? administrativo : tecnica;
			UUID id = id();
			OffsetDateTime criado = momento(data);
			jdbc.update("""
					insert into comunicacao (id, tipo, direcao, data_comunicacao, contraparte, assunto, descricao,
					                         arquivo_id, criado_em, criado_por, atualizado_em)
					values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", id, tipo, direcao, data, contraparte, assunto,
					sorteio.nextInt(100) < 60 ? cifrar(catalogo.um(Catalogo.DESCRICOES_COMUNICACAO)) : null, arquivo,
					criado, autor.id(), criado);
			auditar(criado, autor, "CRIAR", "comunicacao", id);
		}
	}

	// ---------------------------------------------------------------- gestão institucional

	private void gravarEventos() {
		for (LocalDate mes = inicio.withDayOfMonth(1).plusMonths(1); mes.isBefore(hoje); mes = mes.plusMonths(1)) {
			evento("REUNIAO_COORDENACAO_EQUIPE", null, diaUtil(mes.plusDays(2)), "Planejamento mensal das atividades do serviço");
			if (mes.getMonthValue() % 2 == 0) {
				evento("FORMACAO_MULTIPROFISSIONAL", null, diaUtil(mes.plusDays(14)),
						catalogo.um(List.of("Formação sobre comunicação alternativa e aumentativa",
								"Formação em primeiros socorros", "Formação sobre o SUAS e a tipificação dos serviços")));
			}
			if (mes.getMonthValue() % 3 == 1) {
				evento("REUNIAO_DIRECAO_GERAL", null, diaUtil(mes.plusDays(9)), "Prestação de contas à direção");
				evento("REUNIAO_PARCEIROS", catalogo.um(parceiros), diaUtil(mes.plusDays(20)),
						"Alinhamento das ações com a rede parceira");
			}
			if (mes.getMonthValue() % 6 == 3) {
				evento("MONITORAMENTO_ORGAO_GESTOR", null, diaUtil(mes.plusDays(16)),
						"Visita de monitoramento do órgão gestor");
			}
			if (sorteio.nextInt(100) < 45) {
				evento("VISITA_INSTITUCIONAL", null, diaUtil(mes.plusDays(5 + sorteio.nextInt(20))),
						catalogo.um(Catalogo.DESCRICOES_EVENTO));
			}
			if (sorteio.nextInt(100) < 50) {
				evento("EVENTO", null, diaUtil(mes.plusDays(5 + sorteio.nextInt(20))),
						catalogo.um(List.of("Seminário municipal sobre inclusão",
								"Apresentação do serviço para a rede socioassistencial", "Feira de economia solidária",
								"Caminhada pela inclusão")));
			}
		}
	}

	private void evento(String tipo, UUID parceiro, LocalDate data, String descricao) {
		if (!data.isBefore(hoje)) {
			return;
		}
		String direcao = switch (tipo) {
			case "VISITA_INSTITUCIONAL" -> sorteio.nextBoolean() ? "REALIZADO" : "RECEBIDO";
			case "EVENTO" -> sorteio.nextBoolean() ? "REALIZADO" : "PARTICIPADO";
			default -> null;
		};
		String participantes = String.join(", ",
				colaboradores.stream().filter(c -> c.ativoEm(data)).limit(3 + sorteio.nextInt(4)).map(Colaborador::nome)
					.toList());
		UUID id = id();
		OffsetDateTime criado = momento(data);
		jdbc.update("""
				insert into evento_institucional (id, tipo, direcao, data, descricao, participantes, parceiro_id,
				                                  criado_em, criado_por, atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", id, tipo, direcao, data, cifrar(descricao), participantes,
				parceiro, criado, coordenador.id(), criado);
		auditar(criado, coordenador, "CRIAR", "evento_institucional", id);
	}

	private void gravarDoacoes() {
		for (int k = 0; k < 120; k++) {
			LocalDate data = diaUtil(entre(inicio, hoje.minusDays(1)));
			boolean recebida = sorteio.nextInt(100) < 85;
			String tipo = catalogo.um(List.of("DINHEIRO", "DINHEIRO", "ALIMENTOS", "ALIMENTOS", "ALIMENTOS", "VESTUARIO",
					"HIGIENE_LIMPEZA", "HIGIENE_LIMPEZA", "MATERIAL_PEDAGOGICO", "MOVEIS_EQUIPAMENTOS", "OUTRO"));
			String descricao = switch (tipo) {
				case "DINHEIRO" -> "Contribuição em dinheiro para custeio das oficinas";
				case "ALIMENTOS" -> "Cestas básicas e alimentos não perecíveis";
				case "VESTUARIO" -> "Roupas e agasalhos da campanha de inverno";
				case "HIGIENE_LIMPEZA" -> "Kits de higiene pessoal e produtos de limpeza";
				case "MATERIAL_PEDAGOGICO" -> "Papéis, tintas e jogos para as oficinas";
				case "MOVEIS_EQUIPAMENTOS" -> "Mesas, cadeiras e um aparelho de som";
				default -> "Ingressos para passeio cultural";
			};
			String quantidade = switch (tipo) {
				case "DINHEIRO" -> null;
				case "ALIMENTOS" -> (5 + sorteio.nextInt(40)) + " cestas básicas";
				case "VESTUARIO" -> (20 + sorteio.nextInt(80)) + " peças";
				case "HIGIENE_LIMPEZA" -> (10 + sorteio.nextInt(50)) + " kits";
				case "MOVEIS_EQUIPAMENTOS" -> (2 + sorteio.nextInt(10)) + " itens";
				default -> (10 + sorteio.nextInt(40)) + " unidades";
			};
			BigDecimal valor = tipo.equals("DINHEIRO")
					? BigDecimal.valueOf(50 + sorteio.nextInt(60) * 50L).setScale(2, RoundingMode.HALF_UP)
					: null;
			UUID id = id();
			OffsetDateTime criado = momento(data);
			jdbc.update("""
					insert into doacao (id, direcao, data, contraparte, tipo, descricao, quantidade, valor, criado_em,
					                    criado_por, atualizado_em)
					values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", id, recebida ? "RECEBIDA" : "REALIZADA", data,
					catalogo.um(recebida ? Catalogo.DOADORES : Catalogo.BENEFICIARIOS_DOACAO), tipo, descricao,
					quantidade, valor, criado, coordenador.id(), criado);
			auditar(criado, coordenador, "CRIAR", "doacao", id);
		}
	}

	// ---------------------------------------------------------------- notificações e auditoria

	private void gravarNotificacoes() {
		String referencia = candidatosNaFila.isEmpty() ? "/lista-espera" : "/lista-espera/" + candidatosNaFila.getFirst();
		for (int n = 0; n < 6; n++) {
			OffsetDateTime quando = momento(diaUtil(hoje.minusDays(5 + n * 23L)));
			for (String perfil : List.of("COORDENADOR", "ADMINISTRATIVO")) {
				UUID id = id();
				jdbc.update("""
						insert into notificacao (id, perfil_destino, tipo, mensagem, referencia, criada_em, atualizado_em)
						values (?, ?, 'VAGA_ABERTA', ?, ?, ?, ?)""", id, perfil,
						"Uma vaga foi aberta no serviço (1 vaga disponível). Veja o próximo candidato da fila.", referencia,
						quando, quando);
				if (n > 0) {
					Conta leitor = perfil.equals("COORDENADOR") ? coordenador : administrativo;
					jdbc.update("insert into notificacao_leitura (notificacao_id, conta_id, lida_em) values (?, ?, ?)", id,
							leitor.id(), quando.plusHours(2));
				}
			}
		}
	}

	/** Entradas e saídas das contas nos dias úteis, e as exportações mensais de relatório. */
	private void gravarAcessos() {
		for (LocalDate dia = inicio; !dia.isAfter(hoje); dia = dia.plusDays(1)) {
			if (dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
				continue;
			}
			for (Conta conta : List.of(coordenador, tecnica, administrativo, consulta)) {
				if (conta == consulta && dia.getDayOfWeek() != DayOfWeek.MONDAY) {
					continue;
				}
				OffsetDateTime entrada = dia.atTime(7 + sorteio.nextInt(2), sorteio.nextInt(60)).atZone(FUSO)
					.toOffsetDateTime();
				if (sorteio.nextInt(100) < 3) {
					auditar(entrada.minusMinutes(1), conta, "LOGIN_FALHO", "conta_acesso", conta.id());
				}
				auditar(entrada, conta, "LOGIN", "conta_acesso", conta.id());
				if (dia.isBefore(hoje)) {
					auditar(entrada.plusHours(8).plusMinutes(sorteio.nextInt(60)), conta, "LOGOUT", "conta_acesso",
							conta.id());
				}
			}
			if (dia.getDayOfMonth() <= 3 && dia.getDayOfWeek() != DayOfWeek.MONDAY && sorteio.nextBoolean()) {
				auditar(momento(dia), coordenador, "EXPORTAR", "relatorio_estatistico", null);
			}
		}
		for (Atendido atendido : atendidos) {
			LocalDate dia = diaUtil(entre(atendido.insercao(), atendido.fim() == null ? hoje : atendido.fim()));
			if (!dia.isAfter(hoje)) {
				auditar(momento(dia), tecnica, "LER_SENSIVEL", "pessoa", atendido.pessoaId(), "cpf,nis,rg");
			}
		}
	}

	private void gravarAuditoria() {
		auditoria.sort(Comparator.comparing(Registro::quando));
		OffsetDateTime agora = OffsetDateTime.now(FUSO);
		List<Object[]> linhas = auditoria.stream()
			.filter(r -> !r.quando().isAfter(agora))
			.map(r -> new Object[] { id(), r.quando(), r.conta().id(), r.conta().login(), r.acao(), r.entidade(),
					r.entidadeId() == null ? null : r.entidadeId().toString(),
					r.campos() == null ? "{}" : "{" + r.campos() + "}", "10.0.0." + r.conta().ip() })
			.toList();
		jdbc.batchUpdate("""
				insert into auditoria (id, ocorrido_em, conta_id, conta_login, acao, entidade, entidade_id,
				                       campos_alterados, ip)
				values (?, ?, ?, ?, ?, ?, ?, ?::text[], ?)""", linhas);
	}

	// ---------------------------------------------------------------- utilitários

	private void auditar(OffsetDateTime quando, Conta conta, String acao, String entidade, UUID id) {
		auditoria.add(new Registro(quando, conta, acao, entidade, id, null));
	}

	private void auditar(OffsetDateTime quando, Conta conta, String acao, String entidade, UUID id, String campos) {
		auditoria.add(new Registro(quando, conta, acao, entidade, id, campos));
	}

	private List<Colaborador> tecnicos(LocalDate data, int quantos) {
		List<Colaborador> disponiveis = new ArrayList<>(
				colaboradores.stream().filter(c -> c.tecnico() && c.ativoEm(data)).toList());
		Collections.shuffle(disponiveis, sorteio);
		return disponiveis.subList(0, Math.min(quantos, disponiveis.size()));
	}

	private String tipoDeficiencia() {
		int sorte = sorteio.nextInt(100);
		return sorte < 30 ? "INTELECTUAL"
				: sorte < 50 ? "TEA" : sorte < 65 ? "MULTIPLA" : sorte < 80 ? "FISICA" : sorte < 92 ? "T21" : "SENSORIAL";
	}

	private String origem() {
		int sorte = sorteio.nextInt(100);
		return sorte < 30 ? "CRAS"
				: sorte < 45 ? "CREAS"
						: sorte < 55 ? "JUDICIARIO" : sorte < 70 ? "SAUDE" : sorte < 82 ? "EDUCACAO" : "DEMANDA_ESPONTANEA";
	}

	private UUID id() {
		long alto = (sorteio.nextLong() & 0xffffffffffff0fffL) | 0x0000000000004000L;
		long baixo = (sorteio.nextLong() & 0x3fffffffffffffffL) | 0x8000000000000000L;
		return new UUID(alto, baixo);
	}

	/**
	 * Guarda um PDF pelo módulo de arquivos e grava a linha já, porque os inserts seguintes, por JDBC,
	 * referenciam o arquivo.
	 */
	private UUID guardar(String nome, String texto) {
		UUID id = arquivos.guardar(new NovoArquivo(nome, pdf(texto))).id();
		entidades.flush();
		return id;
	}

	private String cifrar(String texto) {
		return cifrador.cifrarTexto(texto);
	}

	private LocalDate entre(LocalDate de, LocalDate ate) {
		long dias = ChronoUnit.DAYS.between(de, ate);
		return dias <= 0 ? de : de.plusDays(sorteio.nextLong(dias + 1));
	}

	private static LocalDate minimo(LocalDate a, LocalDate b) {
		return a.isBefore(b) ? a : b;
	}

	/** Leva sábado e domingo para a sexta-feira anterior, para nenhuma data passar do limite sorteado. */
	private static LocalDate diaUtil(LocalDate data) {
		return switch (data.getDayOfWeek()) {
			case SATURDAY -> data.minusDays(1);
			case SUNDAY -> data.minusDays(2);
			default -> data;
		};
	}

	/** Um horário do expediente na data, das 8h às 17h. */
	private OffsetDateTime momento(LocalDate data) {
		return data.atTime(LocalTime.of(8 + sorteio.nextInt(9), sorteio.nextInt(60))).atZone(FUSO).toOffsetDateTime();
	}

	private static String formatarTelefone(String telefone) {
		return "(" + telefone.substring(0, 2) + ") " + telefone.substring(2, 7) + "-" + telefone.substring(7);
	}

	/** Um PDF mínimo de uma página com uma linha de texto. */
	static byte[] pdf(String texto) {
		String linha = texto.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
		String fluxo = "BT /F1 14 Tf 72 760 Td (" + linha + ") Tj ET";
		List<String> objetos = List.of("<< /Type /Catalog /Pages 2 0 R >>",
				"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
				"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R "
						+ "/Resources << /Font << /F1 5 0 R >> >> >>",
				"<< /Length " + fluxo.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n" + fluxo
						+ "\nendstream",
				"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
		StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
		List<Integer> posicoes = new ArrayList<>();
		for (int i = 0; i < objetos.size(); i++) {
			posicoes.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
			pdf.append(i + 1).append(" 0 obj\n").append(objetos.get(i)).append("\nendobj\n");
		}
		int xref = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
		pdf.append("xref\n0 ").append(objetos.size() + 1).append("\n0000000000 65535 f \n");
		for (int posicao : posicoes) {
			pdf.append("%010d 00000 n \n".formatted(posicao));
		}
		pdf.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
			.append(xref)
			.append("\n%%EOF\n");
		return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
	}

}
