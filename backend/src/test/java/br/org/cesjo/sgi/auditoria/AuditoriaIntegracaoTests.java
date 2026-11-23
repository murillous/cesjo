package br.org.cesjo.sgi.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;

/**
 * Fluxo principal do log de auditoria: uma operação gera o registro, a consulta filtra e só o
 * Coordenador consulta. O banco é compartilhado, então cada teste usa uma entidade e um login próprios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, AuditoriaIntegracaoTests.OperacoesDeTeste.class })
class AuditoriaIntegracaoTests {

	private static final String COORDENADOR = "auditoria-coordenador";

	@Autowired
	MockMvc mvc;

	@Autowired
	Auditoria auditoria;

	@Autowired
	JdbcClient jdbc;

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void operacaoGeraRegistroComContaAcaoEntidadeCamposEIp() throws Exception {
		String entidade = entidadeUnica();
		UUID registroId = UUID.randomUUID();

		mvc.perform(post("/api/teste-auditoria/alteracao").param("entidade", entidade)
			.param("id", registroId.toString())
			.with(csrf())).andExpect(status().isNoContent());

		consultar("entidade", entidade).andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].acao").value("ALTERAR"))
			.andExpect(jsonPath("$.content[0].entidade").value(entidade))
			.andExpect(jsonPath("$.content[0].entidadeId").value(registroId.toString()))
			.andExpect(jsonPath("$.content[0].camposAlterados", contains("nome", "telefone")))
			.andExpect(jsonPath("$.content[0].login").value(COORDENADOR))
			.andExpect(jsonPath("$.content[0].contaId").value(idDaConta(COORDENADOR).toString()))
			.andExpect(jsonPath("$.content[0].ip").value("127.0.0.1"))
			.andExpect(jsonPath("$.content[0].ocorridoEm").isNotEmpty());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void consultaFiltraPorPeriodoLoginEntidadeEAcao() throws Exception {
		String entidade = entidadeUnica();
		String loginA = "auditoria-a-" + UUID.randomUUID();
		String loginB = "auditoria-b-" + UUID.randomUUID();
		UUID contaA = UUID.randomUUID();
		auditoria.registrar(new EventoAuditoria(AcaoAuditoria.CRIAR, entidade, "1", Set.of(), contaA, loginA));
		auditoria.registrar(new EventoAuditoria(AcaoAuditoria.EXCLUIR, entidade, "1", Set.of(), contaA, loginA));
		auditoria.registrar(new EventoAuditoria(AcaoAuditoria.CRIAR, entidade, "2", Set.of(), null, loginB));
		auditoria.registrar(EventoAuditoria.loginFalho(loginB));
		Instant agora = Instant.now();

		consultar("entidade", entidade).andExpect(jsonPath("$.page.totalElements").value(3));
		consultar("entidade", entidade, "acao", "CRIAR").andExpect(jsonPath("$.page.totalElements").value(2));
		consultar("entidade", entidade, "login", loginA.toUpperCase())
			.andExpect(jsonPath("$.page.totalElements").value(2))
			.andExpect(jsonPath("$.content[0].acao").value("EXCLUIR"))
			.andExpect(jsonPath("$.content[1].acao").value("CRIAR"));
		consultar("login", loginB).andExpect(jsonPath("$.page.totalElements").value(2));
		consultar("login", loginB, "acao", "LOGIN_FALHO").andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].entidade").value("conta_acesso"))
			.andExpect(jsonPath("$.content[0].contaId").doesNotExist());
		consultar("entidade", entidade, "de", agora.minus(1, ChronoUnit.HOURS).toString(), "ate",
				agora.plus(1, ChronoUnit.MINUTES).toString())
			.andExpect(jsonPath("$.page.totalElements").value(3));
		consultar("entidade", entidade, "de", agora.plus(1, ChronoUnit.MINUTES).toString())
			.andExpect(jsonPath("$.page.totalElements").value(0));
		consultar("entidade", entidade, "ate", agora.minus(1, ChronoUnit.HOURS).toString())
			.andExpect(jsonPath("$.page.totalElements").value(0));
		consultar("entidade", entidade, "size", "2").andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.page.totalPages").value(2))
			.andExpect(jsonPath("$.page.totalExato").value(true));

		mvc.perform(get("/api/auditoria/entidades")).andExpect(status().isOk()).andExpect(jsonPath("$", hasItem(entidade)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void contaOTotalSoAteOTetoEContinuaPaginando() throws Exception {
		String entidade = entidadeUnica();
		// Mais registros que o teto (ADR 0236), gravados direto: o log é só de inserção.
		jdbc.sql("""
				insert into auditoria (id, ocorrido_em, acao, entidade, entidade_id)
				select gen_random_uuid(), now() - make_interval(secs => n), 'CRIAR', :entidade, n::text
				from generate_series(1, 10005) as n""").param("entidade", entidade).update();

		consultar("entidade", entidade, "size", "50").andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(50)))
			.andExpect(jsonPath("$.content[0].entidadeId").value("1"))
			.andExpect(jsonPath("$.page.totalElements").value(10_000))
			.andExpect(jsonPath("$.page.totalPages").value(200))
			.andExpect(jsonPath("$.page.totalExato").value(false));
		consultar("entidade", entidade, "acao", "CRIAR", "size", "50", "page", "199")
			.andExpect(jsonPath("$.content", hasSize(50)))
			.andExpect(jsonPath("$.content[49].entidadeId").value("10000"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void periodoInvertidoRespondeProblemDetail422() throws Exception {
		Instant agora = Instant.now();
		consultar("de", agora.toString(), "ate", agora.minusSeconds(60).toString())
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.title").value("Regra de negócio violada"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void operacaoDesfeitaNaoDeixaRegistro() throws Exception {
		String entidade = entidadeUnica();

		mvc.perform(post("/api/teste-auditoria/falha").param("entidade", entidade).with(csrf()))
			.andExpect(status().isInternalServerError());

		consultar("entidade", entidade).andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void leituraSensivelEmTransacaoSomenteLeituraEhRegistrada() throws Exception {
		String entidade = entidadeUnica();

		mvc.perform(get("/api/teste-auditoria/leitura").param("entidade", entidade)).andExpect(status().isNoContent());

		consultar("entidade", entidade).andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].acao").value("LER_SENSIVEL"))
			.andExpect(jsonPath("$.content[0].entidadeId").value("42"))
			.andExpect(jsonPath("$.content[0].login").value(COORDENADOR));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void exportacaoForaDeTransacaoEhRegistrada() throws Exception {
		String entidade = entidadeUnica();

		auditoria.registrar(EventoAuditoria.exportacao(entidade));

		consultar("entidade", entidade).andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].acao").value("EXPORTAR"))
			.andExpect(jsonPath("$.content[0].entidadeId").doesNotExist())
			.andExpect(jsonPath("$.content[0].login").value(COORDENADOR));
	}

	@Test
	void nomeDeCampoComValorEhRecusado() {
		String entidade = entidadeUnica();

		assertThatThrownBy(
				() -> auditoria.registrar(EventoAuditoria.alteracao(entidade, 1, Set.of("nome", "nome=Fulano"))))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> auditoria.registrar(EventoAuditoria.criacao("Fulano de Tal", 1)))
			.isInstanceOf(IllegalArgumentException.class);

		assertThat(contarRegistros(entidade)).isZero();
	}

	@Test
	void logEhSoDeInsercao() {
		String entidade = entidadeUnica();
		auditoria.registrar(EventoAuditoria.criacao(entidade, 1));

		assertThatThrownBy(
				() -> jdbc.sql("update auditoria set entidade_id = '2' where entidade = ?").param(entidade).update())
			.isInstanceOf(DataAccessException.class)
			.satisfies(AuditoriaIntegracaoTests::recusadoPeloGatilho);
		assertThatThrownBy(() -> jdbc.sql("delete from auditoria where entidade = ?").param(entidade).update())
			.isInstanceOf(DataAccessException.class)
			.satisfies(AuditoriaIntegracaoTests::recusadoPeloGatilho);

		assertThat(contarRegistros(entidade)).isEqualTo(1);
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "auditoria-equipe")
	void equipeTecnicaNaoConsulta() throws Exception {
		exigirProibido();
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "auditoria-administrativo")
	void administrativoNaoConsulta() throws Exception {
		exigirProibido();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "auditoria-consulta")
	void consultaNaoConsulta() throws Exception {
		exigirProibido();
	}

	private void exigirProibido() throws Exception {
		mvc.perform(get("/api/auditoria")).andExpect(status().isForbidden());
		mvc.perform(get("/api/auditoria/entidades")).andExpect(status().isForbidden());
	}

	/** O gatilho da migração V1 recusa com SQLSTATE 42501 (insufficient_privilege). */
	private static void recusadoPeloGatilho(Throwable erro) {
		assertThat(NestedExceptionUtils.getMostSpecificCause(erro)).isInstanceOfSatisfying(SQLException.class,
				sql -> assertThat(sql.getSQLState()).isEqualTo("42501"));
	}

	private ResultActions consultar(String... parametros) throws Exception {
		var requisicao = get("/api/auditoria");
		for (int i = 0; i < parametros.length; i += 2) {
			requisicao.param(parametros[i], parametros[i + 1]);
		}
		return mvc.perform(requisicao);
	}

	private long contarRegistros(String entidade) {
		return jdbc.sql("select count(*) from auditoria where entidade = ?").param(entidade).query(Long.class).single();
	}

	private static String entidadeUnica() {
		return "teste_" + UUID.randomUUID().toString().replace("-", "");
	}

	private static UUID idDaConta(String login) {
		return UUID.nameUUIDFromBytes(login.getBytes(StandardCharsets.UTF_8));
	}

	/** Operações de mentira que registram auditoria como um módulo de negócio faria. */
	@RestController
	static class OperacoesDeTeste {

		private final Auditoria auditoria;

		OperacoesDeTeste(Auditoria auditoria) {
			this.auditoria = auditoria;
		}

		@PostMapping("/api/teste-auditoria/alteracao")
		@Transactional
		org.springframework.http.ResponseEntity<Void> alteracao(@RequestParam String entidade, @RequestParam UUID id) {
			auditoria.registrar(EventoAuditoria.alteracao(entidade, id, Set.of("telefone", "nome")));
			return org.springframework.http.ResponseEntity.noContent().build();
		}

		@PostMapping("/api/teste-auditoria/falha")
		@Transactional
		void falha(@RequestParam String entidade) {
			auditoria.registrar(EventoAuditoria.criacao(entidade, 1));
			throw new IllegalStateException("operação desfeita");
		}

		@GetMapping("/api/teste-auditoria/leitura")
		@Transactional(readOnly = true)
		org.springframework.http.ResponseEntity<Void> leitura(@RequestParam String entidade) {
			auditoria.registrar(EventoAuditoria.leituraSensivel(entidade, 42));
			return org.springframework.http.ResponseEntity.noContent().build();
		}

	}

}
