package br.org.cesjo.sgi.institucional;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
 * Doações (issue #54, ADR 0178 e ADR 0179): recebidas e realizadas, valor só em dinheiro, listagem por
 * período, permissões e auditoria.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DoacoesTests {

	private static final String URL = "/api/doacoes";

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
	@ComConta(perfil = Perfil.COORDENADOR, login = "doacao-coord-registra")
	void registraDoacaoRecebidaEmDinheiroEAudita() throws Exception {
		Map<String, Object> dados = doacao("RECEBIDA", "2026-05-04", " Mercado Fictício ", "DINHEIRO",
				"Doação para a festa junina.");
		dados.put("valor", 150.5);
		String id = registrar(dados);

		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.direcao").value("RECEBIDA"))
			.andExpect(jsonPath("$.contraparte").value("Mercado Fictício"))
			.andExpect(jsonPath("$.tipo").value("DINHEIRO"))
			.andExpect(jsonPath("$.valor").value(150.5))
			.andExpect(jsonPath("$.quantidade").doesNotExist());
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("doacao")
				&& e.entidadeId().equals(id)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "doacao-coord-realizada")
	void registraDoacaoRealizadaComQuantidadeEEditaAuditandoOsCampos() throws Exception {
		Map<String, Object> dados = doacao("REALIZADA", "2026-06-10", "Família fictícia", "ALIMENTOS",
				"Cesta básica.");
		dados.put("quantidade", "2 cestas");
		String id = registrar(dados);
		clearInvocations(auditoria);

		dados.put("quantidade", "3 cestas");
		editar(id, dados).andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value("3 cestas"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR && e.entidadeId().equals(id)
				&& e.camposAlterados().equals(Set.of("quantidade"))));

		clearInvocations(auditoria);
		editar(id, dados).andExpect(status().isOk());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "doacao-coord-periodo")
	void listaPorPeriodoEDirecao() throws Exception {
		String recebida = registrar(doacao("RECEBIDA", "2024-03-05", "Doador A", "VESTUARIO", "Roupas."));
		String realizada = registrar(doacao("REALIZADA", "2024-03-20", "Instituição B", "OUTRO", "Cadeiras."));
		String fora = registrar(doacao("RECEBIDA", "2024-04-01", "Doador C", "HIGIENE_LIMPEZA", "Kits."));

		mvc.perform(get(URL).param("de", "2024-03-01").param("ate", "2024-03-31").param("size", "1000"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id").value(hasItem(recebida)))
			.andExpect(jsonPath("$.content[*].id").value(hasItem(realizada)))
			.andExpect(jsonPath("$.content[*].id").value(not(hasItem(fora))))
			.andExpect(jsonPath("$.content[0].data").value("2024-03-20"));
		mvc.perform(get(URL).param("de", "2024-03-01").param("ate", "2024-04-30").param("direcao", "RECEBIDA")
			.param("size", "1000"))
			.andExpect(jsonPath("$.content[*].direcao").value(everyItem(is("RECEBIDA"))))
			.andExpect(jsonPath("$.content[*].id").value(hasItem(fora)));
		mvc.perform(get(URL).param("de", "2024-04-30").param("ate", "2024-04-01"))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "doacao-coord-invalido")
	void recusaDadosInvalidos() throws Exception {
		Map<String, Object> valorSemDinheiro = doacao("RECEBIDA", "2026-01-01", "Doador", "ALIMENTOS", "Arroz.");
		valorSemDinheiro.put("valor", 10);
		enviar(valorSemDinheiro).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("valorSoEmDinheiro")));
		Map<String, Object> valorZero = doacao("RECEBIDA", "2026-01-01", "Doador", "DINHEIRO", "Zero.");
		valorZero.put("valor", 0);
		enviar(valorZero).andExpect(status().isBadRequest());
		Map<String, Object> centavos = doacao("RECEBIDA", "2026-01-01", "Doador", "DINHEIRO", "Frações.");
		centavos.put("valor", 1.005);
		enviar(centavos).andExpect(status().isBadRequest());
		enviar(doacao("RECEBIDA", LocalDate.now().plusDays(2).toString(), "Doador", "OUTRO", "Futuro."))
			.andExpect(status().isBadRequest());
		enviar(doacao(null, null, " ", null, " ")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("direcao")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("contraparte")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("tipo")));
		editar(UUID.randomUUID().toString(), doacao("RECEBIDA", "2026-01-01", "Doador", "OUTRO", "Inexistente."))
			.andExpect(status().isNotFound());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "doacao-tecnica")
	void equipeTecnicaSemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "doacao-admin")
	void administrativoSemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "doacao-consulta")
	void consultaSemAcesso() throws Exception {
		semAcesso();
	}

	private void semAcesso() throws Exception {
		String id = UUID.randomUUID().toString();
		Map<String, Object> dados = doacao("RECEBIDA", "2026-01-01", "Doador", "OUTRO", "Doação.");
		mvc.perform(get(URL)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}", id)).andExpect(status().isForbidden());
		enviar(dados).andExpect(status().isForbidden());
		editar(id, dados).andExpect(status().isForbidden());
	}

	private static Map<String, Object> doacao(String direcao, String data, String contraparte, String tipo,
			String descricao) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("direcao", direcao);
		dados.put("data", data);
		dados.put("contraparte", contraparte);
		dados.put("tipo", tipo);
		dados.put("descricao", descricao);
		return dados;
	}

	private ResultActions enviar(Map<String, Object> dados) throws Exception {
		return mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)));
	}

	private String registrar(Map<String, Object> dados) throws Exception {
		String corpo = enviar(dados).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private ResultActions editar(String id, Map<String, Object> dados) throws Exception {
		return mvc.perform(put(URL + "/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)));
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
