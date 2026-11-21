package br.org.cesjo.sgi.listaespera;

import static org.assertj.core.api.Assertions.assertThat;
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
import tools.jackson.databind.json.JsonMapper;

/** Ficha, edição e mudança de situação com histórico (issue #29, ADR 0090 e ADR 0091). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FichaCandidatoTests {

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
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "ficha-dados")
	void fichaTemPosicaoEHistoricoInicial() throws Exception {
		String id = cadastrar("Ficha Inicial Fictícia");
		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posicao").isNumber())
			.andExpect(jsonPath("$.historico.length()").value(1))
			.andExpect(jsonPath("$.historico[0].situacaoNova").value("AGUARDANDO"))
			.andExpect(jsonPath("$.historico[0].data").value("2025-06-01"));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "ficha-edicao")
	void equipeTecnicaEditaComAuditoriaDosCampos() throws Exception {
		String id = inserirDireto("Edição Antes Fictícia");
		clearInvocations(auditoria);
		Map<String, Object> dados = dados("Edição Depois Fictícia");
		dados.put("telefone", "(31) 3234-5678");
		dados.put("origemEncaminhamento", "EDUCACAO");

		mvc.perform(put(URL + "/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nomeCompleto").value("Edição Depois Fictícia"))
			.andExpect(jsonPath("$.origemEncaminhamento").value("EDUCACAO"))
			.andExpect(jsonPath("$.nomeResponsavel").value("Responsável Fictício"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR && e.entidade().equals("pessoa")
				&& e.camposAlterados().contains("nome_completo") && !e.camposAlterados().contains("telefone")));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidade().equals("inscricao_lista_espera") && e.camposAlterados().equals(java.util.Set.of("origem_encaminhamento"))));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "ficha-edicao-invalida")
	void edicaoValidaComoOCadastro() throws Exception {
		String id = cadastrar("Edição Inválida Fictícia");
		Map<String, Object> dados = dados(" ");
		dados.put("telefone", "123");
		mvc.perform(put(URL + "/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "ficha-saida")
	void saiDaFilaComMotivoEVoltaParaAPosicaoOriginal() throws Exception {
		String id = cadastrar("Saída Fictícia");
		Integer posicao = JsonPath.read(mvc.perform(get(URL + "/{id}", id)).andReturn().getResponse().getContentAsString(),
				"$.posicao");

		mudar(id, Map.of("situacao", "SAIU", "data", "2025-07-01", "motivoSaida", "NAO_LOCALIZADO",
				"observacao", "Telefone não atende há três semanas."))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.situacao").value("SAIU"))
			.andExpect(jsonPath("$.motivoSaida").value("NAO_LOCALIZADO"))
			.andExpect(jsonPath("$.dataSaida").value("2025-07-01"))
			.andExpect(jsonPath("$.posicao").doesNotExist())
			.andExpect(jsonPath("$.historico.length()").value(2))
			.andExpect(jsonPath("$.historico[1].observacao").value("Telefone não atende há três semanas."));
		assertThat(jdbc.queryForObject(
				"select observacao from historico_situacao_inscricao where inscricao_id = ?::uuid and situacao_nova = 'SAIU'",
				String.class, id))
			.startsWith("v1:");

		mudar(id, Map.of("situacao", "AGUARDANDO", "data", "2025-07-10", "observacao", "Família retomou contato."))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.situacao").value("AGUARDANDO"))
			.andExpect(jsonPath("$.motivoSaida").doesNotExist())
			.andExpect(jsonPath("$.posicao").value(posicao))
			.andExpect(jsonPath("$.historico.length()").value(3))
			.andExpect(jsonPath("$.historico[2].situacaoAnterior").value("SAIU"));
		verify(auditoria, org.mockito.Mockito.times(2)).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidade().equals("inscricao_lista_espera") && e.camposAlterados().contains("situacao")));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "ficha-regras")
	void recusaMudancasForaDasRegras() throws Exception {
		String id = cadastrar("Regras Fictícia");
		mudar(id, Map.of("situacao", "SAIU", "data", "2025-07-01")).andExpect(status().isUnprocessableContent());
		mudar(id, Map.of("situacao", "SAIU", "data", "2025-07-01", "motivoSaida", "OUTRO"))
			.andExpect(status().isUnprocessableContent());
		mudar(id, Map.of("situacao", "SAIU", "data", "2025-05-01", "motivoSaida", "DESISTENCIA"))
			.andExpect(status().isUnprocessableContent());
		mudar(id, Map.of("situacao", "AGUARDANDO", "data", "2025-07-01", "observacao", "x"))
			.andExpect(status().isUnprocessableContent());
		mudar(id, Map.of("situacao", "INSERIDO", "data", "2025-07-01")).andExpect(status().isUnprocessableContent());
		mudar(id, Map.of("situacao", "SAIU", "data", "2025-07-01", "motivoSaida", "DESISTENCIA"))
			.andExpect(status().isOk());
		mudar(id, Map.of("situacao", "AGUARDANDO", "data", "2025-07-02")).andExpect(status().isUnprocessableContent());
		mudar(id, Map.of("situacao", "SAIU", "data", "2099-01-01", "motivoSaida", "DESISTENCIA"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "ficha-consulta")
	void consultaNaoEditaNemMudaSituacao() throws Exception {
		UUID id = UUID.randomUUID();
		mvc.perform(put(URL + "/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados("X"))))
			.andExpect(status().isForbidden());
		mudar(id.toString(), Map.of("situacao", "SAIU", "data", "2025-07-01", "motivoSaida", "DESISTENCIA"))
			.andExpect(status().isForbidden());
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR));
	}

	private ResultActions mudar(String id, Map<String, Object> corpo) throws Exception {
		return mvc.perform(post(URL + "/{id}/situacao", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(new HashMap<>(corpo))));
	}

	private String cadastrar(String nome) throws Exception {
		String corpo = mvc
			.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados(nome))))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	/** A Equipe Técnica não cadastra; o candidato entra direto no banco, como a migração V7 deixaria. */
	private String inserirDireto(String nome) {
		UUID pessoa = UUID.randomUUID();
		UUID inscricao = UUID.randomUUID();
		jdbc.update("insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)"
				+ " values (?, ?, 'TEA', '3132345678', now(), now())", pessoa, nome);
		jdbc.update("insert into responsavel (id, pessoa_id, nome_completo, criado_em, atualizado_em)"
				+ " values (?, ?, 'Responsável Fictício', now(), now())", UUID.randomUUID(), pessoa);
		jdbc.update("insert into inscricao_lista_espera (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao,"
				+ " criado_em, atualizado_em) values (?, ?, date '2025-06-01', 'CRAS', 'AGUARDANDO', now(), now())",
				inscricao, pessoa);
		return inscricao.toString();
	}

	private static Map<String, Object> dados(String nome) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("nomeCompleto", nome);
		dados.put("tipoDeficiencia", "TEA");
		dados.put("telefone", "3132345678");
		dados.put("nomeResponsavel", "Responsável Fictício");
		dados.put("origemEncaminhamento", "CRAS");
		dados.put("dataCadastro", "2025-06-01");
		return dados;
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
