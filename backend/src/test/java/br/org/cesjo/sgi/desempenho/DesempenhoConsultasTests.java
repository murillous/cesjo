package br.org.cesjo.sgi.desempenho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.org.cesjo.sgi.acesso.AutenticacaoDeTeste;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.compartilhado.criptografia.Cifrador;

/**
 * Teste de desempenho das consultas (issue #25, RNF 3.2, ADR 0230 a ADR 0232): grava uma massa
 * simulada de 10 vezes o volume esperado num banco só dele e mede listagens, relatórios e dashboards
 * com 20 usuários simultâneos. Toda requisição precisa responder em até 3 segundos.
 *
 * <p>
 * Fica fora do {@code ./mvnw verify} normal pela tag {@code desempenho}; roda com
 * {@code ./mvnw verify -Pdesempenho}. Parâmetros opcionais por propriedade de sistema:
 * {@code desempenho.fator} (10), {@code desempenho.usuarios} (20), {@code desempenho.rodadas} (5) e
 * {@code desempenho.indices-propostos} (falso).
 * O resultado sai no console e em {@code target/desempenho/resultado.md}. Só dados fictícios.
 */
@Tag("desempenho")
@Import(BancoDoDesempenho.class)
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class DesempenhoConsultasTests {

	private static final Duration LIMITE = Duration.ofSeconds(3);

	private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

	private static final int FATOR = Integer.getInteger("desempenho.fator", 10);

	private static final int USUARIOS = Integer.getInteger("desempenho.usuarios", 20);

	private static final int RODADAS = Integer.getInteger("desempenho.rodadas", 5);

	/**
	 * Com {@code -Ddesempenho.indices-propostos=true}, cria no banco do teste o índice proposto no
	 * ADR 0234, sem migração, para medir o ganho antes de propor a migração.
	 */
	private static final boolean INDICES_PROPOSTOS = Boolean.getBoolean("desempenho.indices-propostos");

	/** Índice proposto no ADR 0234 para a auditoria filtrada por login. Não é migração. */
	private static final List<String> SQL_INDICES_PROPOSTOS = List.of("""
			create index auditoria_login_cobertura_idx
			    on auditoria (lower(conta_login), ocorrido_em desc, id desc) include (conta_login)""",
			"drop index auditoria_login_idx", "vacuum analyze auditoria");

	/** Requisições de aquecimento por consulta antes da medição (JIT, caches do Hibernate e do Postgres). */
	private static final int AQUECIMENTO = 3;

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	Cifrador cifrador;

	/** Uma consulta medida: grupo do critério de aceite, nome curto e URL com os parâmetros. */
	record Consulta(String grupo, String nome, String url) {
	}

	/**
	 * Tempos de uma consulta em milissegundos: a mediana sozinha, sem carga, e os tempos sob carga, já
	 * ordenados.
	 */
	record Resultado(Consulta consulta, double isolado, List<Double> tempos) {

		double percentil(double p) {
			int indice = (int) Math.ceil(p / 100 * tempos.size()) - 1;
			return tempos.get(Math.max(0, indice));
		}

		double maximo() {
			return tempos.get(tempos.size() - 1);
		}

	}

	@Test
	void consultasRespondemEmAte3SegundosCom20UsuariosSimultaneos() throws Exception {
		MassaSimulada.Volume volume = MassaSimulada.ESPERADO.vezes(FATOR);
		long inicioMassa = System.nanoTime();
		Map<String, Long> linhas = new MassaSimulada(jdbc, cifrador.cifrarTexto("Texto fictício cifrado da massa."))
			.gravar(volume, HOJE);
		if (INDICES_PROPOSTOS) {
			SQL_INDICES_PROPOSTOS.forEach(jdbc::execute);
		}
		Duration tempoMassa = Duration.ofNanos(System.nanoTime() - inicioMassa);

		List<Consulta> consultas = consultas();
		Authentication coordenador = AutenticacaoDeTeste.de(Perfil.COORDENADOR, "desempenho-coord");
		for (int i = 0; i < AQUECIMENTO; i++) {
			for (Consulta consulta : consultas) {
				executar(consulta, coordenador);
			}
		}
		Map<Consulta, Double> isolados = new ConcurrentHashMap<>();
		for (Consulta consulta : consultas) {
			List<Double> sozinha = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				long inicio = System.nanoTime();
				executar(consulta, coordenador);
				sozinha.add((System.nanoTime() - inicio) / 1_000_000.0);
			}
			Collections.sort(sozinha);
			isolados.put(consulta, sozinha.get(2));
		}

		Map<Consulta, Queue<Double>> tempos = new ConcurrentHashMap<>();
		consultas.forEach(consulta -> tempos.put(consulta, new ConcurrentLinkedQueue<>()));
		Queue<String> falhas = new ConcurrentLinkedQueue<>();
		CountDownLatch largada = new CountDownLatch(1);
		long inicioCarga;
		try (ExecutorService usuarios = Executors.newFixedThreadPool(USUARIOS)) {
			List<Future<?>> tarefas = new ArrayList<>();
			for (int u = 0; u < USUARIOS; u++) {
				int usuario = u;
				tarefas.add(usuarios.submit(() -> {
					Authentication conta = AutenticacaoDeTeste.de(Perfil.COORDENADOR, "desempenho-" + usuario);
					Random aleatorio = new Random(usuario);
					largada.await();
					for (int rodada = 0; rodada < RODADAS; rodada++) {
						List<Consulta> ordem = new ArrayList<>(consultas);
						Collections.shuffle(ordem, aleatorio);
						for (Consulta consulta : ordem) {
							long inicio = System.nanoTime();
							int status = executar(consulta, conta).getResponse().getStatus();
							tempos.get(consulta).add((System.nanoTime() - inicio) / 1_000_000.0);
							if (status != 200) {
								falhas.add(consulta.url() + " respondeu " + status);
							}
						}
					}
					return null;
				}));
			}
			inicioCarga = System.nanoTime();
			largada.countDown();
			for (Future<?> tarefa : tarefas) {
				tarefa.get();
			}
		}
		Duration tempoCarga = Duration.ofNanos(System.nanoTime() - inicioCarga);

		List<Resultado> resultados = consultas.stream().map(consulta -> {
			List<Double> ordenados = new ArrayList<>(tempos.get(consulta));
			Collections.sort(ordenados);
			return new Resultado(consulta, isolados.get(consulta), ordenados);
		}).toList();
		String relatorio = relatorio(volume, linhas, tempoMassa, tempoCarga, resultados);
		System.out.println(relatorio);
		Path arquivo = Path.of("target", "desempenho", "resultado.md");
		Files.createDirectories(arquivo.getParent());
		Files.writeString(arquivo, relatorio);

		assertThat(falhas).isEmpty();
		assertThat(resultados).allSatisfy(resultado -> assertThat(resultado.maximo())
			.as("tempo máximo de %s", resultado.consulta().url())
			.isLessThanOrEqualTo(LIMITE.toMillis()));
	}

	/** Listagens, relatórios e dashboards da issue #25, com e sem filtros. */
	private static List<Consulta> consultas() {
		String umAnoAtras = HOJE.minusYears(1).toString();
		String inicioDoMes = HOJE.withDayOfMonth(1).toString();
		String cincoAnosAtras = HOJE.minusYears(5).toString();
		String pessoa = MassaSimulada.uuid("pessoa3");
		String colaborador = MassaSimulada.uuid("colaborador1");
		YearMonth mes = YearMonth.from(HOJE);
		return List.of(new Consulta("Listagem", "Lista de espera", "/api/lista-espera/inscricoes"),
				new Consulta("Listagem", "Lista de espera, busca por nome e situação",
						"/api/lista-espera/inscricoes?nome=silva&situacao=AGUARDANDO"),
				new Consulta("Listagem", "Lista de espera, página 50", "/api/lista-espera/inscricoes?page=50"),
				new Consulta("Listagem", "Usuários atendidos", "/api/atendidos"),
				new Consulta("Listagem", "Usuários atendidos, filtros da ficha completa",
						"/api/atendidos?situacao=ATIVA&faixaEtaria=ADULTO&genero=FEMININO&bairro=fict"),
				new Consulta("Listagem", "Usuários atendidos, busca por nome", "/api/atendidos?nome=souza&page=5"),
				new Consulta("Listagem", "Atividades", "/api/atividades"),
				new Consulta("Listagem", "Atividades, último ano e tipo",
						"/api/atividades?tipo=COM_USUARIOS&de=" + umAnoAtras + "&ate=" + HOJE),
				new Consulta("Listagem", "Atividades de um usuário", "/api/atividades?pessoaId=" + pessoa),
				new Consulta("Listagem", "Atividades de um profissional",
						"/api/atividades?profissionalId=" + colaborador + "&page=10"),
				new Consulta("Listagem", "Comunicações", "/api/comunicacoes"),
				new Consulta("Listagem", "Comunicações, período e contraparte",
						"/api/comunicacoes?inicio=" + umAnoAtras + "&fim=" + HOJE + "&contraparte=fict%C3%ADcia%201"),
				new Consulta("Listagem", "Auditoria", "/api/auditoria"),
				new Consulta("Listagem", "Auditoria, login e página 100", "/api/auditoria?login=conta.ficticia7&page=100"),
				new Consulta("Listagem", "Auditoria, entidade, ação e período",
						"/api/auditoria?entidade=atividade&acao=CRIAR&de=" + HOJE.minusYears(2)
								+ "T00:00:00Z&ate=" + HOJE + "T00:00:00Z"),
				new Consulta("Relatório", "Relatório mensal, mês corrente", "/api/estatisticas/relatorios/mensal?mes=" + mes),
				new Consulta("Relatório", "Relatório mensal, 2 anos atrás",
						"/api/estatisticas/relatorios/mensal?mes=" + mes.minusYears(2)),
				new Consulta("Relatório", "Relatório quadrimestral, quadrimestre corrente",
						"/api/estatisticas/relatorios/quadrimestral?ano=" + HOJE.getYear() + "&quadrimestre="
								+ ((HOJE.getMonthValue() - 1) / 4 + 1)),
				new Consulta("Relatório", "Relatório anual, ano passado",
						"/api/estatisticas/relatorios/anual?ano=" + (HOJE.getYear() - 1)),
				new Consulta("Relatório", "Relatório de transparência, 12 meses",
						"/api/estatisticas/relatorios/transparencia?inicio=" + umAnoAtras + "&fim=" + HOJE),
				new Consulta("Relatório", "Relatório de intervenção, 12 meses",
						"/api/estatisticas/relatorios/intervencao?inicio=" + umAnoAtras + "&fim=" + HOJE),
				new Consulta("Dashboard", "Perfil dos atendidos", "/api/estatisticas/dashboards/perfil"),
				new Consulta("Dashboard", "Atendimento, mês corrente",
						"/api/estatisticas/dashboards/atendimento?inicio=" + inicioDoMes + "&fim=" + HOJE),
				new Consulta("Dashboard", "Atendimento, 5 anos",
						"/api/estatisticas/dashboards/atendimento?inicio=" + cincoAnosAtras + "&fim=" + HOJE));
	}

	private MvcResult executar(Consulta consulta, Authentication conta) throws Exception {
		return mvc.perform(get(URI.create(consulta.url())).with(authentication(conta))).andReturn();
	}

	private static String relatorio(MassaSimulada.Volume volume, Map<String, Long> linhas, Duration tempoMassa,
			Duration tempoCarga, List<Resultado> resultados) {
		StringBuilder texto = new StringBuilder();
		Runtime runtime = Runtime.getRuntime();
		texto.append("## Resultado do teste de desempenho\n\n");
		texto.append("- Data: ").append(HOJE).append('\n');
		texto.append("- Máquina: ")
			.append(System.getProperty("os.name"))
			.append(' ')
			.append(System.getProperty("os.arch"))
			.append(", ")
			.append(runtime.availableProcessors())
			.append(" processadores lógicos, Java ")
			.append(System.getProperty("java.version"))
			.append(", heap máximo ")
			.append(runtime.maxMemory() / (1024 * 1024))
			.append(" MB\n");
		texto.append("- Massa: ")
			.append(FATOR)
			.append(" vezes o volume esperado de ")
			.append(volume.anos())
			.append(" anos, gravada em ")
			.append(tempoMassa.toSeconds())
			.append(" s\n");
		texto.append("- Índice proposto no ADR 0234: ").append(INDICES_PROPOSTOS ? "criado" : "não criado").append('\n');
		texto.append("- Carga: ")
			.append(USUARIOS)
			.append(" usuários simultâneos, ")
			.append(RODADAS)
			.append(" rodadas de todas as consultas por usuário, em ordem aleatória, depois de ")
			.append(AQUECIMENTO)
			.append(" rodadas de aquecimento; duração ")
			.append(tempoCarga.toSeconds())
			.append(" s\n\n");
		texto.append("| Tabela | Linhas |\n|---|---:|\n");
		linhas.forEach((tabela, total) -> texto.append("| `").append(tabela).append("` | ").append(total).append(" |\n"));
		texto.append("\n| Grupo | Consulta | Sozinha, mediana (ms) | Requisições sob carga | p50 (ms) | p95 (ms) | Máximo (ms) |\n");
		texto.append("|---|---|---:|---:|---:|---:|---:|\n");
		for (Resultado resultado : resultados) {
			texto.append("| ")
				.append(resultado.consulta().grupo())
				.append(" | ")
				.append(resultado.consulta().nome())
				.append(" | ")
				.append(formatar(resultado.isolado()))
				.append(" | ")
				.append(resultado.tempos().size())
				.append(" | ")
				.append(formatar(resultado.percentil(50)))
				.append(" | ")
				.append(formatar(resultado.percentil(95)))
				.append(" | ")
				.append(formatar(resultado.maximo()))
				.append(" |\n");
		}
		return texto.toString();
	}

	private static String formatar(double milissegundos) {
		return String.format(Locale.ROOT, "%.0f", milissegundos);
	}

}
