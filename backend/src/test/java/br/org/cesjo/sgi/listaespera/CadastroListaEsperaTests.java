package br.org.cesjo.sgi.listaespera;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

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

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import tools.jackson.databind.json.JsonMapper;

/** Cadastro de candidato na lista de espera (issue #26): validação, permissões e auditoria. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CadastroListaEsperaTests {

	private static final String URL = "/api/lista-espera/inscricoes";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "lista-espera-admin")
	void cadastraCandidatoAguardandoComPessoaEResponsavel() throws Exception {
		String corpo = mvc
			.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dadosValidos())))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(URL + "/")))
			.andExpect(jsonPath("$.nomeCompleto").value("Pessoa Fictícia"))
			.andExpect(jsonPath("$.nomeResponsavel").value("Responsável Fictício"))
			.andExpect(jsonPath("$.telefone").value("31987654321"))
			.andExpect(jsonPath("$.tipoDeficiencia").value("TEA"))
			.andExpect(jsonPath("$.origemEncaminhamento").value("CRAS"))
			.andExpect(jsonPath("$.dataCadastro").value("2026-11-02"))
			.andExpect(jsonPath("$.situacao").value("AGUARDANDO"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String id = JsonPath.read(corpo, "$.id");
		String pessoaId = JsonPath.read(corpo, "$.pessoaId");

		mvc.perform(get(URL + "/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.pessoaId").value(pessoaId));
		org.assertj.core.api.Assertions
			.assertThat(jdbc.queryForObject("select count(*) from responsavel where pessoa_id = ?::uuid", Long.class,
					pessoaId))
			.isEqualTo(1);
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR
				&& e.entidade().equals("inscricao_lista_espera") && e.entidadeId().equals(id)));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("pessoa")
				&& e.entidadeId().equals(pessoaId)));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("responsavel")));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "lista-espera-coord")
	void dataDeNascimentoEOpcional() throws Exception {
		Map<String, Object> dados = dadosValidos();
		dados.remove("dataNascimento");
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.dataNascimento").doesNotExist());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "lista-espera-invalido")
	void recusaDadosObrigatoriosAusentesOuInvalidos() throws Exception {
		Map<String, Object> dados = dadosValidos();
		dados.put("nomeCompleto", " ");
		dados.remove("nomeResponsavel");
		dados.put("telefone", "12345");
		dados.remove("tipoDeficiencia");
		dados.remove("origemEncaminhamento");
		dados.put("dataCadastro", LocalDate.now().plusDays(2).toString());

		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("nomeCompleto")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("nomeResponsavel")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("telefone")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("tipoDeficiencia")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("origemEncaminhamento")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("dataCadastro")));
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "lista-espera-tecnica")
	void equipeTecnicaNaoCadastra() throws Exception {
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dadosValidos())))
			.andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "lista-espera-consulta")
	void consultaNaoCadastraNemLeAFicha() throws Exception {
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dadosValidos())))
			.andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}", java.util.UUID.randomUUID())).andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "lista-espera-tecnica-leitura")
	void inscricaoInexistenteResponde404() throws Exception {
		mvc.perform(get(URL + "/{id}", java.util.UUID.randomUUID())).andExpect(status().isNotFound());
	}

	private static Map<String, Object> dadosValidos() {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("nomeCompleto", "  Pessoa Fictícia ");
		dados.put("dataNascimento", "1990-05-10");
		dados.put("tipoDeficiencia", "TEA");
		dados.put("telefone", "(31) 98765-4321");
		dados.put("nomeResponsavel", "Responsável Fictício");
		dados.put("origemEncaminhamento", "CRAS");
		dados.put("dataCadastro", "2026-11-02");
		return dados;
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
