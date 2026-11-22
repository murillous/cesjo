package br.org.cesjo.sgi.atendidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cadastro completo do usuário atendido (issue #40): dados pessoais, gênero, faixa etária, bairro,
 * crença e saúde só para quem pode, documentos e saúde cifrados, auditoria só com nomes de campos.
 * Só dados fictícios; o CPF é gerado só para passar no dígito verificador.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CadastroCompletoTests {

	private static final LocalDate HOJE = AtendidosDeTeste.HOJE;

	/** CPF fictício com dígitos verificadores válidos. */
	private static final String CPF = "529.982.247-25";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@AfterEach
	void restaurarAuditoria() {
		reset(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "cadastro-coord")
	void completaOCadastroCifraOsSensiveisEAudita() throws Exception {
		String id = inserir("Cadastro Ana", HOJE.minusYears(25));
		clearInvocations(auditoria);

		salvar(id, edicao("Cadastro Ana Maria", HOJE.minusYears(35), true)).andExpect(status().isNoContent());

		mvc.perform(get("/api/atendidos/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nomeCompleto").value("Cadastro Ana Maria"))
			.andExpect(jsonPath("$.faixaEtaria").value("ADULTO"))
			.andExpect(jsonPath("$.cadastro.genero").value("FEMININO"))
			.andExpect(jsonPath("$.cadastro.cpf").value("52998224725"))
			.andExpect(jsonPath("$.cadastro.cep").value("36000000"))
			.andExpect(jsonPath("$.cadastro.bairro").value("Centro"))
			.andExpect(jsonPath("$.cadastro.rendaFamiliar").value(1518.5))
			.andExpect(jsonPath("$.cadastro.recebeBpc").value(true))
			.andExpect(jsonPath("$.dadosSensiveis.crencaReligiosa").value("MATRIZ_AFRICANA"))
			.andExpect(jsonPath("$.dadosSensiveis.medicamentosUsoContinuo").value("Remédio fictício, 1 por dia"));

		String pessoaId = pessoaId(id);
		Map<String, Object> linha = jdbc.queryForMap("select * from cadastro_complementar where pessoa_id = ?::uuid",
				pessoaId);
		assertThat(linha.get("genero")).isEqualTo("FEMININO");
		assertThat(linha.get("bairro")).isEqualTo("Centro");
		assertThat(linha.get("crenca_religiosa")).isEqualTo("MATRIZ_AFRICANA");
		assertThat((String) linha.get("cpf")).startsWith("v1:").doesNotContain("52998224725");
		assertThat((String) linha.get("nis")).startsWith("v1:");
		assertThat((String) linha.get("medicamentos_uso_continuo")).startsWith("v1:").doesNotContain("Remédio");

		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR && e.entidade().equals("pessoa")
				&& e.camposAlterados().containsAll(java.util.Set.of("nome_completo", "data_nascimento"))));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR
				&& e.entidade().equals("cadastro_complementar") && e.entidadeId().equals(pessoaId)));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidade().equals("cadastro_complementar") && e.camposAlterados().contains("crenca_religiosa")));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL
				&& e.entidade().equals("cadastro_complementar") && e.entidadeId().equals(pessoaId)));

		clearInvocations(auditoria);
		Map<String, Object> mudanca = edicao("Cadastro Ana Maria", HOJE.minusYears(35), true);
		cadastro(mudanca).put("bairro", "São Mateus");
		salvar(id, mudanca).andExpect(status().isNoContent());
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidade().equals("cadastro_complementar") && e.camposAlterados().equals(java.util.Set.of("bairro"))));
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR && e.entidade().equals("pessoa")));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "cadastro-filtros")
	void listagemFiltraPorGeneroEBairro() throws Exception {
		String prefixo = "Cad" + UUID.randomUUID().toString().substring(0, 6);
		String id = inserir(prefixo + " Bia", HOJE.minusYears(20));
		inserir(prefixo + " Caio", HOJE.minusYears(40));
		salvar(id, edicao(prefixo + " Bia", HOJE.minusYears(20), false)).andExpect(status().isNoContent());

		mvc.perform(get("/api/atendidos").param("nome", prefixo).param("genero", "FEMININO"))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].genero").value("FEMININO"))
			.andExpect(jsonPath("$.content[0].bairro").value("Centro"))
			.andExpect(jsonPath("$.content[0].faixaEtaria").value("JOVEM"));
		mvc.perform(get("/api/atendidos").param("nome", prefixo).param("bairro", "cent"))
			.andExpect(jsonPath("$.content", hasSize(1)));
		mvc.perform(get("/api/atendidos").param("nome", prefixo)).andExpect(jsonPath("$.content", hasSize(2)));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "cadastro-tecnica")
	void equipeTecnicaCompletaOCadastroComCrenca() throws Exception {
		String id = inserir("Cadastro Técnica", HOJE.minusYears(30));

		salvar(id, edicao("Cadastro Técnica", HOJE.minusYears(30), true)).andExpect(status().isNoContent());
		mvc.perform(get("/api/atendidos/{id}", id))
			.andExpect(jsonPath("$.dadosSensiveis.crencaReligiosa").value("MATRIZ_AFRICANA"));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "cadastro-admin")
	void administrativoNaoEditaNemVeCrenca() throws Exception {
		String id = inserir("Cadastro Admin", HOJE.minusYears(30));

		salvar(id, edicao("Cadastro Admin", HOJE.minusYears(30), false)).andExpect(status().isForbidden());
		mvc.perform(get("/api/atendidos/{id}", id))
			.andExpect(jsonPath("$.completo").value(false))
			.andExpect(jsonPath("$.cadastro").doesNotExist())
			.andExpect(jsonPath("$.dadosSensiveis").doesNotExist());
		mvc.perform(get("/api/atendidos").param("genero", "FEMININO")).andExpect(status().isForbidden());
		mvc.perform(get("/api/atendidos").param("bairro", "Centro")).andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "cadastro-consulta")
	void consultaNaoEdita() throws Exception {
		String id = inserir("Cadastro Consulta", HOJE.minusYears(30));

		salvar(id, edicao("Cadastro Consulta", HOJE.minusYears(30), false)).andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "cadastro-validacao")
	void recusaCpfInvalidoECepMalFormado() throws Exception {
		String id = inserir("Cadastro Validação", HOJE.minusYears(30));

		Map<String, Object> cpfErrado = edicao("Cadastro Validação", HOJE.minusYears(30), false);
		cadastro(cpfErrado).put("cpf", "111.111.111-11");
		salvar(id, cpfErrado).andExpect(status().isBadRequest());

		Map<String, Object> cepErrado = edicao("Cadastro Validação", HOJE.minusYears(30), false);
		cadastro(cepErrado).put("cep", "123");
		salvar(id, cepErrado).andExpect(status().isBadRequest());

		salvar(UUID.randomUUID().toString(), edicao("Ninguém", null, false)).andExpect(status().isNotFound());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "cadastro-sem-sensiveis")
	void semDadosSensiveisMantemACrenca() throws Exception {
		String id = inserir("Cadastro Mantém", HOJE.minusYears(30));
		salvar(id, edicao("Cadastro Mantém", HOJE.minusYears(30), true)).andExpect(status().isNoContent());

		salvar(id, edicao("Cadastro Mantém", HOJE.minusYears(30), false)).andExpect(status().isNoContent());
		mvc.perform(get("/api/atendidos/{id}", id))
			.andExpect(jsonPath("$.dadosSensiveis.crencaReligiosa").value("MATRIZ_AFRICANA"));
	}

	private String inserir(String nome, LocalDate nascimento) throws Exception {
		return new AtendidosDeTeste(mvc, jdbc).inserirNoServico(nome, nascimento, "TEA", "MATUTINO");
	}

	private String pessoaId(String insercaoId) {
		return jdbc.queryForObject("select pessoa_id::text from insercao where id = ?::uuid", String.class, insercaoId);
	}

	private ResultActions salvar(String id, Object corpo) throws Exception {
		return mvc.perform(put("/api/atendidos/{id}/cadastro", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(corpo)));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> cadastro(Map<String, Object> edicao) {
		return (Map<String, Object>) edicao.get("cadastro");
	}

	private static Map<String, Object> edicao(String nome, LocalDate nascimento, boolean comSensiveis) {
		Map<String, Object> cadastro = new LinkedHashMap<>();
		cadastro.put("genero", "FEMININO");
		cadastro.put("cpf", CPF);
		cadastro.put("nis", "12345678901");
		cadastro.put("rg", "MG-00.000.000");
		cadastro.put("escolaridade", "FUNDAMENTAL_COMPLETO");
		cadastro.put("cep", "36000-000");
		cadastro.put("logradouro", "Rua Fictícia");
		cadastro.put("numero", "100");
		cadastro.put("complemento", null);
		cadastro.put("bairro", "Centro");
		cadastro.put("cidade", "Juiz de Fora");
		cadastro.put("rendaFamiliar", 1518.5);
		cadastro.put("recebeBpc", true);
		cadastro.put("inscritoCadUnico", true);
		cadastro.put("recebeBolsaFamilia", false);

		Map<String, Object> edicao = new LinkedHashMap<>();
		edicao.put("nomeCompleto", nome);
		edicao.put("dataNascimento", nascimento == null ? null : nascimento.toString());
		edicao.put("tipoDeficiencia", "TEA");
		edicao.put("telefone", "(31) 91234-5678");
		edicao.put("cadastro", cadastro);
		if (comSensiveis) {
			Map<String, Object> sensiveis = new LinkedHashMap<>();
			sensiveis.put("crencaReligiosa", "MATRIZ_AFRICANA");
			sensiveis.put("medicamentosUsoContinuo", "Remédio fictício, 1 por dia");
			sensiveis.put("observacoesSaude", null);
			edicao.put("dadosSensiveis", sensiveis);
		}
		return edicao;
	}

}
