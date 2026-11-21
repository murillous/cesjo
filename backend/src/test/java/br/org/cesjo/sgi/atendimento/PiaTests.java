package br.org.cesjo.sgi.atendimento;

import static org.assertj.core.api.Assertions.assertThat;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
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

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import tools.jackson.databind.json.JsonMapper;

/** Elaboração e edição do PIA (issue #37): conteúdo cifrado, leitura auditada e permissões. */
@Import({ TestcontainersConfiguration.class, DadosDeVisita.class })
@SpringBootTest
@AutoConfigureMockMvc
class PiaTests {

	static final String URL = "/api/pias";

	@Autowired
	MockMvc mvc;

	@Autowired
	DadosDeVisita dados;

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
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "pia-tecnica-elabora")
	void elaboraPiaCifradoELeituraAuditada() throws Exception {
		UUID pessoa = dados.candidato("Pessoa Com PIA", "EM_AVALIACAO");

		String corpo = mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", pessoa, "conteudo", conteudo("Ampliar a autonomia (fictício)")))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.numeroVersao").value(1))
			.andExpect(jsonPath("$.status").value("ELABORADO"))
			.andExpect(jsonPath("$.editavel").value(true))
			.andExpect(jsonPath("$.nomePessoa").value("Pessoa Com PIA"))
			.andExpect(jsonPath("$.conteudo.objetivos[0].descricao").value("Ampliar a autonomia (fictício)"))
			.andExpect(jsonPath("$.conteudo.objetivos[0].prazo").value("2027-03-31"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String id = JsonPath.read(corpo, "$.id");

		assertThat(jdbc.queryForObject("select conteudo from pia where id = ?::uuid", String.class, id)).startsWith("v1:")
			.doesNotContain("autonomia");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("pia")));

		mvc.perform(get(URL).param("pessoaId", pessoa.toString()))
			.andExpect(jsonPath("$[0].id").value(id))
			.andExpect(jsonPath("$[0].conteudo").doesNotExist());
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL));
		mvc.perform(get(URL + "/{id}", id)).andExpect(jsonPath("$.conteudo.diagnostico").value("Diagnóstico fictício"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL && e.entidadeId().equals(id)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "pia-coord-edita")
	void editaEnquantoElaborado() throws Exception {
		UUID pessoa = dados.candidato("Pessoa PIA Editado", "EM_AVALIACAO");
		String id = elaborar(pessoa);
		clearInvocations(auditoria);

		mvc.perform(put(URL + "/{id}", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(conteudo("Objetivo revisto"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.conteudo.objetivos[0].descricao").value("Objetivo revisto"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.camposAlterados().equals(Set.of("conteudo"))));

		clearInvocations(auditoria);
		mvc.perform(put(URL + "/{id}", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(conteudo("Objetivo revisto"))))
			.andExpect(status().isOk());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));

		jdbc.update("update pia set status = 'APRESENTADO_FAMILIA' where id = ?::uuid", id);
		mvc.perform(put(URL + "/{id}", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(conteudo("Outra mudança"))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Só um PIA em elaboração pode ser editado. Crie uma nova versão."));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "pia-tecnica-regras")
	void recusaSegundoPiaObjetivoSemDescricaoEPessoaInexistente() throws Exception {
		UUID pessoa = dados.candidato("Pessoa PIA Regras", "AGUARDANDO");
		elaborar(pessoa);
		clearInvocations(auditoria);

		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", pessoa, "conteudo", conteudo("Outro")))))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", UUID.randomUUID(), "conteudo", conteudo("Objetivo")))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("A pessoa escolhida não existe."));
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", pessoa, "conteudo", conteudo(" ")))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("conteudo.objetivos[0].descricao")));
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "pia-admin")
	void administrativoNaoTemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "pia-consulta")
	void consultaNaoTemAcesso() throws Exception {
		semAcesso();
	}

	private void semAcesso() throws Exception {
		mvc.perform(get(URL).param("pessoaId", UUID.randomUUID().toString())).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", UUID.randomUUID(), "conteudo", conteudo("x")))))
			.andExpect(status().isForbidden());
		mvc.perform(put(URL + "/{id}", UUID.randomUUID()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(conteudo("x"))))
			.andExpect(status().isForbidden());
	}

	String elaborar(UUID pessoa) throws Exception {
		String corpo = mvc
			.perform(post(URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("pessoaId", pessoa, "conteudo", conteudo("Objetivo inicial")))))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	static Map<String, Object> conteudo(String descricaoObjetivo) {
		Map<String, Object> objetivo = new LinkedHashMap<>();
		objetivo.put("descricao", descricaoObjetivo);
		objetivo.put("metas", "Meta fictícia");
		objetivo.put("acoes", "Ação fictícia");
		objetivo.put("prazo", "2027-03-31");
		objetivo.put("responsavel", "Equipe técnica");
		Map<String, Object> conteudo = new LinkedHashMap<>();
		conteudo.put("diagnostico", "Diagnóstico fictício");
		conteudo.put("objetivos", List.of(objetivo));
		conteudo.put("observacoes", null);
		return conteudo;
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
