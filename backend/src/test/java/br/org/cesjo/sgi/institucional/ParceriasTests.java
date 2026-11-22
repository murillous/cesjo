package br.org.cesjo.sgi.institucional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
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
 * Parcerias (issue #53, ADR 0172 e ADR 0173): cadastro de parceiros, ações de articulação, permissões
 * e auditoria. O Postgres dos testes é compartilhado, então cada teste cria os próprios dados.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ParceriasTests {

	private static final String URL = "/api/parceiros";

	@Autowired
	MockMvc mvc;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "parc-coord-cria")
	void cadastraParceiroComTodosOsCamposEAudita() throws Exception {
		Map<String, Object> dados = parceiro("  CRAS Centro Fictício ", "ORGAO_PUBLICO");
		dados.put("instrumento", "Termo de cooperação 12/2026");
		dados.put("vigenciaInicio", "2026-01-01");
		dados.put("vigenciaFim", "2027-12-31");
		dados.put("contato", "Coordenação do CRAS, (00) 0000-0000");
		dados.put("observacoes", "Encaminhamentos mensais.");
		String id = criar(dados);

		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value("CRAS Centro Fictício"))
			.andExpect(jsonPath("$.tipo").value("ORGAO_PUBLICO"))
			.andExpect(jsonPath("$.instrumento").value("Termo de cooperação 12/2026"))
			.andExpect(jsonPath("$.vigenciaFim").value("2027-12-31"))
			.andExpect(jsonPath("$.ativo").value(true));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("parceiro")
				&& e.entidadeId().equals(id)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "parc-coord-edita")
	void edicaoAuditaSoOsCamposAlteradosEFiltraASituacao() throws Exception {
		String id = criar(parceiro("Empresa Parceira Fictícia", "EMPRESA"));
		clearInvocations(auditoria);

		Map<String, Object> edicao = parceiro("Empresa Parceira Fictícia", "EMPRESA");
		edicao.put("contato", "contato@exemplo.org");
		edicao.put("ativo", false);
		editar(id, edicao).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR && e.entidadeId().equals(id)
				&& e.camposAlterados().equals(Set.of("contato", "ativo"))));

		clearInvocations(auditoria);
		editar(id, edicao).andExpect(status().isOk());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));

		mvc.perform(get(URL).param("ativo", "true").param("size", "1000"))
			.andExpect(jsonPath("$.content[*].id").value(not(hasItem(id))));
		mvc.perform(get(URL).param("nome", "parceira fict").param("size", "1000"))
			.andExpect(jsonPath("$.content[*].id").value(hasItem(id)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "parc-coord-articula")
	void registraAcoesDeArticulacaoEmOrdemDeData() throws Exception {
		String id = criar(parceiro("Universidade Fictícia", "INSTITUICAO_ENSINO"));
		clearInvocations(auditoria);

		articular(id, "2026-03-10", "Reunião para estágio supervisionado.").andExpect(status().isCreated())
			.andExpect(jsonPath("$.descricao").value("Reunião para estágio supervisionado."));
		articular(id, "2026-05-02", "Assinatura do termo.").andExpect(status().isCreated());
		verify(auditoria, org.mockito.Mockito.times(2))
			.registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("articulacao_parceria")));

		mvc.perform(get(URL + "/{id}/articulacoes", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].data").value("2026-05-02"))
			.andExpect(jsonPath("$[1].data").value("2026-03-10"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "parc-coord-invalido")
	void recusaDadosInvalidos() throws Exception {
		Map<String, Object> dados = parceiro(" ", null);
		dados.put("vigenciaInicio", "2026-05-01");
		dados.put("vigenciaFim", "2026-04-30");
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("nome")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("tipo")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("vigenciaCoerente")));

		String id = criar(parceiro("Parceiro Válido", "OUTRO"));
		clearInvocations(auditoria);
		articular(id, LocalDate.now().plusDays(2).toString(), "Futuro.").andExpect(status().isBadRequest());
		articular(id, "2026-01-01", " ").andExpect(status().isBadRequest());
		articular(UUID.randomUUID().toString(), "2026-01-01", "Sem parceiro.").andExpect(status().isNotFound());
		editar(id, parceiro("Sem situação", "OUTRO")).andExpect(status().isUnprocessableContent());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "parc-tecnica")
	void equipeTecnicaSemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "parc-admin")
	void administrativoSemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "parc-consulta")
	void consultaSemAcesso() throws Exception {
		semAcesso();
	}

	private void semAcesso() throws Exception {
		String id = UUID.randomUUID().toString();
		mvc.perform(get(URL)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}", id)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}/articulacoes", id)).andExpect(status().isForbidden());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(parceiro("Fictício", "OUTRO"))))
			.andExpect(status().isForbidden());
		Map<String, Object> edicao = parceiro("Fictício", "OUTRO");
		edicao.put("ativo", true);
		editar(id, edicao).andExpect(status().isForbidden());
		articular(id, "2026-01-01", "Ação.").andExpect(status().isForbidden());
	}

	private static Map<String, Object> parceiro(String nome, String tipo) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("nome", nome);
		dados.put("tipo", tipo);
		return dados;
	}

	private String criar(Map<String, Object> dados) throws Exception {
		String corpo = mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith(URL + "/")))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private ResultActions editar(String id, Map<String, Object> dados) throws Exception {
		return mvc.perform(put(URL + "/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)));
	}

	private ResultActions articular(String id, String data, String descricao) throws Exception {
		return mvc.perform(post(URL + "/{id}/articulacoes", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("data", data, "descricao", descricao))));
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
