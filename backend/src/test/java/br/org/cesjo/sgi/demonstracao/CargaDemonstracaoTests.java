package br.org.cesjo.sgi.demonstracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.compartilhado.criptografia.Cifrador;
import br.org.cesjo.sgi.desempenho.BancoDoDesempenho;

/**
 * Carga de demonstração (ADR 0237): sobe a aplicação com o perfil {@code demo} num banco só deste teste
 * e confere que a massa grava, que todo texto cifrado decifra e que as contas entram e leem as telas.
 * Fica no {@code verify} normal para a carga acompanhar as migrações.
 */
@ActiveProfiles("demo")
@Import(BancoDoDesempenho.class)
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class CargaDemonstracaoTests {

	/** Colunas cifradas (ADR 0004), como tabela.coluna. */
	private static final List<String> CIFRADAS = List.of("arquivo.nome_original", "desligamento.observacao",
			"violacao_direito.descricao", "escuta.dinamica_familiar", "escuta.necessidades_imediatas",
			"escuta.habilidades", "escuta.potencialidades", "pia.conteudo", "visita_domiciliar.observacoes",
			"atividade.descricao", "comunicacao.descricao", "evento_institucional.descricao",
			"vinculo_colaborador.motivo_desligamento", "historico_situacao_inscricao.observacao",
			"cadastro_complementar.cpf", "cadastro_complementar.nis", "cadastro_complementar.rg",
			"cadastro_complementar.medicamentos_uso_continuo", "cadastro_complementar.observacoes_saude");

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	Cifrador cifrador;

	@Autowired
	CargaDemonstracao carga;

	@Test
	void gravaAMassaComOVolumeCombinado() {
		int inseridos = GeradorDemonstracao.ATIVOS + GeradorDemonstracao.DESLIGADOS;
		assertThat(contar("select count(*) from conta_acesso")).isEqualTo(4);
		assertThat(contar("select count(*) from pessoa")).isEqualTo(inseridos + GeradorDemonstracao.AGUARDANDO
				+ GeradorDemonstracao.EM_AVALIACAO + GeradorDemonstracao.SAIRAM);
		assertThat(contar("select count(*) from insercao where situacao = 'ATIVA'")).isEqualTo(GeradorDemonstracao.ATIVOS);
		assertThat(contar("select count(*) from desligamento")).isEqualTo(GeradorDemonstracao.DESLIGADOS);
		assertThat(contar("select count(*) from inscricao_lista_espera where situacao in ('AGUARDANDO', 'EM_AVALIACAO')"))
			.isEqualTo(GeradorDemonstracao.AGUARDANDO + GeradorDemonstracao.EM_AVALIACAO);
		assertThat(contar("select count(*) from atividade")).isBetween(250L, 450L);
		assertThat(contar("select count(*) from comunicacao")).isBetween(150L, 350L);
		assertThat(contar("select count(*) from auditoria")).isGreaterThan(3_000L);
	}

	@Test
	void todoTextoCifradoDecifra() {
		for (String alvo : CIFRADAS) {
			String[] partes = alvo.split("\\.");
			List<String> valores = jdbc.queryForList(
					"select " + partes[1] + " from " + partes[0] + " where " + partes[1] + " is not null", String.class);
			assertThat(valores).as(alvo).isNotEmpty();
			valores.forEach(valor -> assertThat(cifrador.decifrarTexto(valor)).as(alvo).isNotBlank());
		}
	}

	@Test
	void cadaPerfilEntraSemTrocarASenha() throws Exception {
		for (String login : List.of("coordenador", "equipe.tecnica", "administrativo", "consulta")) {
			entrar(login);
		}
	}

	@Test
	void coordenadorLeFichasEPaineis() throws Exception {
		MockHttpSession sessao = entrar("coordenador");
		UUID pia = jdbc.queryForObject("select id from pia where status = 'APROVADO' limit 1", UUID.class);
		mvc.perform(get("/api/pias/" + pia).session(sessao))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.conteudo.objetivos").isNotEmpty());
		UUID escuta = jdbc.queryForObject("select id from escuta limit 1", UUID.class);
		mvc.perform(get("/api/escutas/" + escuta).session(sessao)).andExpect(status().isOk());
		mvc.perform(get("/api/estatisticas/dashboards/perfil").session(sessao)).andExpect(status().isOk());
		mvc.perform(get("/api/estatisticas/dashboards/atendimento").param("inicio", java.time.LocalDate.now().minusYears(1).toString())
			.param("fim", java.time.LocalDate.now().toString())
			.session(sessao)).andExpect(status().isOk());
	}

	@Test
	void naoGravaDeNovoQuandoOBancoJaTemDados() throws Exception {
		long antes = contar("select count(*) from pessoa");
		carga.run(null);
		assertThat(contar("select count(*) from pessoa")).isEqualTo(antes);
	}

	private MockHttpSession entrar(String login) throws Exception {
		return (MockHttpSession) mvc
			.perform(post("/api/sessao/entrar").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"login\":\"" + login + "\",\"senha\":\"" + CargaDemonstracao.SENHA_PADRAO + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trocaSenhaObrigatoria").value(false))
			.andReturn()
			.getRequest()
			.getSession(false);
	}

	private long contar(String sql) {
		return jdbc.queryForObject(sql, Long.class);
	}

}
