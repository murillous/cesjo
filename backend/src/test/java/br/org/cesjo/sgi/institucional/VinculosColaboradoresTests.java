package br.org.cesjo.sgi.institucional;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Contratações e desligamentos dos colaboradores (issue #51, ADR 0170): histórico de vínculos, efeito
 * na lista de profissionais para novos agendamentos, permissões, cifra do motivo e auditoria.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class VinculosColaboradoresTests {

	private static final String URL = "/api/colaboradores";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	Colaboradores colaboradores;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "vinc-coord-fluxo")
	void contrataDesligaERecontrataComHistorico() throws Exception {
		UUID id = criar("Colaboradora Vínculo", "2025-01-10");
		assertThat(colaboradores.obterAtivo(id)).isPresent();

		clearInvocations(auditoria);
		desligar(id, "2026-03-31", "Pedido de demissão.").andExpect(status().isOk())
			.andExpect(jsonPath("$.dataContratacao").value("2025-01-10"))
			.andExpect(jsonPath("$.dataDesligamento").value("2026-03-31"))
			.andExpect(jsonPath("$.motivoDesligamento").value("Pedido de demissão."));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidade().equals("vinculo_colaborador")
				&& e.camposAlterados().equals(Set.of("data_desligamento", "motivo_desligamento"))));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidade().equals(Colaboradores.ENTIDADE) && e.camposAlterados().equals(Set.of("ativo"))));

		// Desligado some das listas de profissionais para novos agendamentos.
		assertThat(colaboradores.obterAtivo(id)).isEmpty();
		assertThat(colaboradores.listarAtivos()).extracting(ResumoColaborador::id).doesNotContain(id);
		mvc.perform(get(URL + "/{id}", id)).andExpect(jsonPath("$.ativo").value(false));

		// O motivo fica cifrado no banco.
		String gravado = jdbc.queryForObject(
				"select motivo_desligamento from vinculo_colaborador where colaborador_id = ?", String.class, id);
		assertThat(gravado).doesNotContain("demissão");

		clearInvocations(auditoria);
		contratar(id, "2026-06-01").andExpect(status().isCreated());
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR
				&& e.entidade().equals("vinculo_colaborador")));
		assertThat(colaboradores.obterAtivo(id)).isPresent();

		mvc.perform(get(URL + "/{id}/vinculos", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].dataContratacao").value("2026-06-01"))
			.andExpect(jsonPath("$[0].dataDesligamento").doesNotExist())
			.andExpect(jsonPath("$[1].dataContratacao").value("2025-01-10"))
			.andExpect(jsonPath("$[1].dataDesligamento").value("2026-03-31"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "vinc-coord-regras")
	void recusaDatasEVinculosIncoerentes() throws Exception {
		UUID semVinculo = criar("Sem Vínculo", null);
		clearInvocations(auditoria);
		desligar(semVinculo, "2026-01-01", "Fim de contrato.").andExpect(status().isUnprocessableContent());

		UUID id = criar("Com Vínculo", "2025-05-01");
		clearInvocations(auditoria);
		contratar(id, "2025-06-01").andExpect(status().isUnprocessableContent());
		desligar(id, "2025-04-30", "Fim de contrato.").andExpect(status().isUnprocessableContent());
		desligar(id, LocalDate.now().plusDays(2).toString(), "Fim de contrato.").andExpect(status().isBadRequest());
		desligar(id, "2025-12-31", " ").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(org.hamcrest.Matchers.hasItem("motivo")));
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));

		desligar(id, "2025-12-31", "Fim de contrato.").andExpect(status().isOk());
		contratar(id, "2025-12-30").andExpect(status().isUnprocessableContent());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "vinc-coord-reativa")
	void desligadoSoVoltaPorNovaContratacao() throws Exception {
		UUID id = criar("Desligado Fictício", "2025-02-01");
		desligar(id, "2025-08-01", "Fim de contrato.").andExpect(status().isOk());

		mvc.perform(put(URL + "/{id}", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("nome", "Desligado Fictício", "funcao", "OUTRA", "ativo", true))))
			.andExpect(status().isUnprocessableContent());

		// Sem histórico de vínculos, a situação continua livre, como antes da issue #51.
		UUID antigo = criar("Antigo Sem Vínculo", null);
		Map<String, Object> inativar = new LinkedHashMap<>(Map.of("nome", "Antigo Sem Vínculo", "funcao", "OUTRA", "ativo", false));
		mvc.perform(put(URL + "/{id}", antigo).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(inativar)))
			.andExpect(status().isOk());
		inativar.put("ativo", true);
		mvc.perform(put(URL + "/{id}", antigo).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(inativar)))
			.andExpect(status().isOk());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "vinc-tecnica")
	void equipeTecnicaSemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "vinc-admin")
	void administrativoSemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "vinc-consulta")
	void consultaSemAcesso() throws Exception {
		semAcesso();
	}

	private void semAcesso() throws Exception {
		UUID id = UUID.randomUUID();
		mvc.perform(get(URL + "/{id}/vinculos", id)).andExpect(status().isForbidden());
		contratar(id, "2025-01-01").andExpect(status().isForbidden());
		desligar(id, "2025-01-01", "Motivo.").andExpect(status().isForbidden());
	}

	private UUID criar(String nome, String dataContratacao) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("nome", nome);
		dados.put("funcao", "OUTRA");
		dados.put("dataContratacao", dataContratacao);
		String corpo = mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return UUID.fromString(JsonPath.read(corpo, "$.id"));
	}

	private ResultActions contratar(UUID id, String data) throws Exception {
		return mvc.perform(post(URL + "/{id}/contratacoes", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("dataContratacao", data))));
	}

	private ResultActions desligar(UUID id, String data, String motivo) throws Exception {
		return mvc.perform(post(URL + "/{id}/desligamento", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("data", data, "motivo", motivo))));
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
