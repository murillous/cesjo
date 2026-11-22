package br.org.cesjo.sgi.atividades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contatos telefônicos e atendimento a público não acompanhado (issue #48, RF011): registro, correção,
 * listagem, permissões e contagem. Só dados fictícios. Os testes que contam usam datas antigas próprias,
 * para não somar os registros dos outros testes.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ContatosTests {

	private static final String URL = "/api/comunicacoes";

	private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	Comunicacoes comunicacoes;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "contato-tecnica")
	void registraContatoComDescricaoCifrada() throws Exception {
		String corpo = registrar(dados("CONTATO_CUIDADOR_FAMILIAR", HOJE, " Mãe Fictícia de Ana ",
				" Faltas da semana ", " Relato fictício do contato. "))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.tipo").value("CONTATO_CUIDADOR_FAMILIAR"))
			.andExpect(jsonPath("$.contraparte").value("Mãe Fictícia de Ana"))
			.andExpect(jsonPath("$.assunto").value("Faltas da semana"))
			.andExpect(jsonPath("$.descricao").value("Relato fictício do contato."))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String id = JsonPath.read(corpo, "$.id");

		String gravada = jdbc.queryForObject("select descricao from comunicacao where id = ?::uuid", String.class, id);
		assertThat(gravada).startsWith("v1:").doesNotContain("Relato");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR
				&& e.entidade().equals(Comunicacoes.ENTIDADE) && e.entidadeId().equals(id)));

		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.descricao").value("Relato fictício do contato."));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "contato-sem-descricao")
	void descricaoEOpcional() throws Exception {
		String corpo = registrar(dados("ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO", HOJE, "Pessoa Fictícia do Balcão",
				"Informações sobre o serviço", "  "))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.descricao").doesNotExist())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String id = JsonPath.read(corpo, "$.id");
		assertThat(jdbc.queryForObject("select descricao from comunicacao where id = ?::uuid", String.class, id))
			.isNull();
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "contato-admin-corrige")
	void administrativoCorrigeEAuditoriaGuardaSoOsCamposAlterados() throws Exception {
		Map<String, Object> original = dados("CONTATO_INSTITUCIONAL", HOJE, "CRAS Fictício", "Vaga para usuário",
				null);
		String id = JsonPath.read(registrar(original).andReturn().getResponse().getContentAsString(), "$.id");
		clearInvocations(auditoria);

		Map<String, Object> corrigido = new LinkedHashMap<>(original);
		corrigido.put("assunto", "Vaga para usuário novo");
		corrigido.put("descricao", "Retorno combinado.");
		corrigir(id, corrigido).andExpect(status().isOk())
			.andExpect(jsonPath("$.assunto").value("Vaga para usuário novo"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.camposAlterados().equals(Set.of("assunto", "descricao"))));

		clearInvocations(auditoria);
		corrigir(id, corrigido).andExpect(status().isOk());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "contato-invalido")
	void recusaDadosInvalidos() throws Exception {
		registrar(dados(null, HOJE.plusDays(1), " ", "", null)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("tipo")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("data")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("contraparte")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("assunto")));
		corrigir(UUID.randomUUID().toString(), dados("CONTATO_INSTITUCIONAL", HOJE, "CREAS Fictício", "Assunto", null))
			.andExpect(status().isNotFound());
		mvc.perform(get(URL).param("inicio", "2020-02-01").param("fim", "2020-01-01"))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "contato-lista")
	void listaPorTipoEPeriodoSemADescricao() throws Exception {
		LocalDate dia = LocalDate.of(2002, 3, 10);
		registrar(dados("CONTATO_INSTITUCIONAL", dia, "CREAS Fictício", "Encaminhamento de caso", "Sigiloso."));
		registrar(dados("CONTATO_CUIDADOR_FAMILIAR", dia.plusDays(1), "Pai Fictício", "Transporte", null));
		registrar(dados("CONTATO_INSTITUCIONAL", dia.plusMonths(2), "Escola Fictícia", "Fora do período", null));

		mvc.perform(get(URL).param("tipo", "CONTATO_INSTITUCIONAL")
			.param("inicio", "2002-03-01")
			.param("fim", "2002-03-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].contraparte").value("CREAS Fictício"))
			.andExpect(jsonPath("$.content[0].descricao").doesNotExist());
		mvc.perform(get(URL).param("inicio", "2002-03-01").param("fim", "2002-03-31"))
			.andExpect(jsonPath("$.page.totalElements").value(2))
			.andExpect(jsonPath("$.content[0].contraparte").value("Pai Fictício"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "contato-contagem")
	void contaPorTipoNoPeriodoComZeroNosTiposSemRegistro() throws Exception {
		LocalDate dia = LocalDate.of(2003, 5, 1);
		registrar(dados("CONTATO_CUIDADOR_FAMILIAR", dia, "Mãe Fictícia", "Assunto", null));
		registrar(dados("CONTATO_CUIDADOR_FAMILIAR", dia.plusDays(30), "Avó Fictícia", "Assunto", null));
		registrar(dados("CONTATO_INSTITUCIONAL", dia.plusDays(5), "UBS Fictícia", "Assunto", null));
		registrar(dados("CONTATO_INSTITUCIONAL", dia.plusMonths(1), "Fora do mês", "Assunto", null));

		assertThat(comunicacoes.contarPorTipo(dia, dia.withDayOfMonth(31)))
			.containsEntry(TipoComunicacao.CONTATO_CUIDADOR_FAMILIAR, 2L)
			.containsEntry(TipoComunicacao.CONTATO_INSTITUCIONAL, 1L)
			.containsEntry(TipoComunicacao.ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO, 0L)
			.hasSize(TipoComunicacao.values().length);
		assertThatThrownBy(() -> comunicacoes.contarPorTipo(dia, dia.minusDays(1)))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "contato-consulta")
	void consultaNaoVeNemRegistra() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
		registrar(dados("CONTATO_INSTITUCIONAL", HOJE, "CRAS Fictício", "Assunto", null))
			.andExpect(status().isForbidden());
	}

	private ResultActions registrar(Map<String, Object> dados) throws Exception {
		return mvc.perform(
				post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados)));
	}

	private ResultActions corrigir(String id, Map<String, Object> dados) throws Exception {
		return mvc.perform(put(URL + "/{id}", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(dados)));
	}

	private static Map<String, Object> dados(String tipo, LocalDate data, String contraparte, String assunto,
			String descricao) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("tipo", tipo);
		dados.put("data", data.toString());
		dados.put("contraparte", contraparte);
		dados.put("assunto", assunto);
		dados.put("descricao", descricao);
		return dados;
	}

}
