package br.org.cesjo.sgi.atendimento;

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

import java.time.LocalDate;
import java.time.ZoneId;
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

/** Status, aprovação e novas versões do PIA (issue #38): ordem dos passos, datas, permissões e auditoria. */
@Import({ TestcontainersConfiguration.class, DadosDeVisita.class })
@SpringBootTest
@AutoConfigureMockMvc
class AprovacaoPiaTests {

	private static final String URL = PiaTests.URL;

	private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

	@Autowired
	MockMvc mvc;

	@Autowired
	DadosDeVisita dados;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "pia-coord-aprova")
	void percorreOsPassosComDataEAprova() throws Exception {
		String id = elaborar("Pessoa PIA Aprovado");

		passo(id, "apresentar-a-familia", HOJE.minusDays(5)).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APRESENTADO_FAMILIA"))
			.andExpect(jsonPath("$.apresentadoFamiliaEm").value(HOJE.minusDays(5).toString()))
			.andExpect(jsonPath("$.editavel").value(false));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.camposAlterados().equals(Set.of("status", "apresentado_familia_em"))));

		mvc.perform(put(URL + "/{id}", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(PiaTests.conteudo("Mudança"))))
			.andExpect(status().isUnprocessableContent());

		passo(id, "apresentar-a-equipe", HOJE.minusDays(6)).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("A data não pode ser anterior à do passo anterior do PIA."));
		passo(id, "apresentar-a-equipe", HOJE.minusDays(2)).andExpect(status().isOk());
		passo(id, "aprovar", HOJE.plusDays(1)).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("A data não pode ser futura."));
		clearInvocations(auditoria);
		passo(id, "aprovar", HOJE).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APROVADO"))
			.andExpect(jsonPath("$.aprovadoEm").value(HOJE.toString()));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.camposAlterados().equals(Set.of("status", "aprovado_em", "aprovado_por"))));
		passo(id, "aprovar", HOJE).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("O PIA já está aprovado. Para mudar, crie uma nova versão."));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "pia-tecnica-ordem")
	void equipeTecnicaApresentaMasNaoAprovaNemPulaPassos() throws Exception {
		String id = elaborar("Pessoa PIA Ordem");

		passo(id, "apresentar-a-equipe", HOJE).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("O PIA em elaboração precisa ser apresentado à família primeiro."));
		passo(id, "apresentar-a-familia", HOJE).andExpect(status().isOk());
		passo(id, "apresentar-a-equipe", HOJE).andExpect(status().isOk());
		passo(id, "aprovar", HOJE).andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "pia-coord-nova-versao")
	void mudancaDePiaAprovadoGeraNovaVersaoEPreservaAAnterior() throws Exception {
		String v1 = elaborar("Pessoa PIA Versões");
		mvc.perform(post(URL + "/{id}/nova-versao", v1).with(csrf())).andExpect(status().isUnprocessableContent());
		passo(v1, "apresentar-a-familia", HOJE).andExpect(status().isOk());
		passo(v1, "apresentar-a-equipe", HOJE).andExpect(status().isOk());
		passo(v1, "aprovar", HOJE).andExpect(status().isOk());
		clearInvocations(auditoria);

		String corpo = mvc.perform(post(URL + "/{id}/nova-versao", v1).with(csrf()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.numeroVersao").value(2))
			.andExpect(jsonPath("$.status").value("ELABORADO"))
			.andExpect(jsonPath("$.versaoAnteriorId").value(v1))
			.andExpect(jsonPath("$.conteudo.objetivos[0].descricao").value("Objetivo inicial"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String v2 = JsonPath.read(corpo, "$.id");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidadeId().equals(v2)));

		mvc.perform(put(URL + "/{id}", v2).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(PiaTests.conteudo("Objetivo da versão 2"))))
			.andExpect(status().isOk());
		mvc.perform(get(URL + "/{id}", v1))
			.andExpect(jsonPath("$.status").value("APROVADO"))
			.andExpect(jsonPath("$.maisNova").value(false))
			.andExpect(jsonPath("$.conteudo.objetivos[0].descricao").value("Objetivo inicial"));
		mvc.perform(post(URL + "/{id}/nova-versao", v1).with(csrf()))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Esta versão do PIA foi substituída por uma mais nova e não muda mais."));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "pia-admin-passos")
	void administrativoNaoMexeNoStatus() throws Exception {
		UUID id = UUID.randomUUID();
		passo(id.toString(), "apresentar-a-familia", HOJE).andExpect(status().isForbidden());
		passo(id.toString(), "aprovar", HOJE).andExpect(status().isForbidden());
		mvc.perform(post(URL + "/{id}/nova-versao", id).with(csrf())).andExpect(status().isForbidden());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	private String elaborar(String nome) throws Exception {
		UUID pessoa = dados.candidato(nome, "EM_AVALIACAO");
		String corpo = mvc
			.perform(post(URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(
						Map.of("pessoaId", pessoa, "conteudo", PiaTests.conteudo("Objetivo inicial")))))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private ResultActions passo(String id, String acao, LocalDate data) throws Exception {
		return mvc.perform(post(URL + "/{id}/" + acao, id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("data", data.toString()))));
	}

}
