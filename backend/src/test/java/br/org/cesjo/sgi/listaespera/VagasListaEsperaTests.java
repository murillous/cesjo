package br.org.cesjo.sgi.listaespera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/**
 * Capacidade e vagas disponíveis (issue #30, ADR 0092, ADR 0093 e ADR 0135). O banco é compartilhado com outros
 * testes, então cada teste parte da ocupação e da fila que o painel informa.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class VagasListaEsperaTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	Auditoria auditoria;

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "vagas-coord")
	void coordenadorDefineACapacidadeEVeAsVagasDisponiveis() throws Exception {
		long ocupadas = ocupadas();
		definir(ocupadas + 3).andExpect(status().isOk())
			.andExpect(jsonPath("$.capacidade").value(ocupadas + 3))
			.andExpect(jsonPath("$.ocupadas").value(ocupadas))
			.andExpect(jsonPath("$.disponiveis").value(3))
			.andExpect(jsonPath("$.definidaEm").isNotEmpty());
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("capacidade_servico")));

		ocuparVaga();
		mvc.perform(get("/api/lista-espera/vagas"))
			.andExpect(jsonPath("$.ocupadas").value(ocupadas + 1))
			.andExpect(jsonPath("$.disponiveis").value(2));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "vagas-lotado")
	void vagasDisponiveisNuncaFicamNegativas() throws Exception {
		ocuparVaga();
		definir(0).andExpect(jsonPath("$.disponiveis").value(0));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "vagas-fila")
	void painelMostraOTamanhoDaFila() throws Exception {
		long antes = numero(mvc.perform(get("/api/lista-espera/vagas")), "$.tamanhoFila");
		inserirInscricao("AGUARDANDO");
		inserirInscricao("EM_AVALIACAO");
		inserirInscricao("INSERIDO");
		assertThat(numero(mvc.perform(get("/api/lista-espera/vagas")), "$.tamanhoFila")).isEqualTo(antes + 2);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "vagas-invalida")
	void recusaCapacidadeInvalida() throws Exception {
		definirCorpo("{\"vagas\": -1}").andExpect(status().isBadRequest());
		definirCorpo("{}").andExpect(status().isBadRequest());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "vagas-admin")
	void administrativoVeMasNaoDefine() throws Exception {
		mvc.perform(get("/api/lista-espera/vagas")).andExpect(status().isOk());
		definir(10).andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "vagas-consulta")
	void consultaVeOPainel() throws Exception {
		mvc.perform(get("/api/lista-espera/vagas")).andExpect(status().isOk());
		definir(10).andExpect(status().isForbidden());
	}

	private long ocupadas() throws Exception {
		return numero(mvc.perform(get("/api/lista-espera/vagas")), "$.ocupadas");
	}

	private static long numero(ResultActions resultado, String caminho) throws Exception {
		return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), caminho)).longValue();
	}

	private ResultActions definir(long vagas) throws Exception {
		return definirCorpo("{\"vagas\": " + vagas + "}");
	}

	private ResultActions definirCorpo(String corpo) throws Exception {
		return mvc.perform(put("/api/lista-espera/capacidade").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	/** Usuário atendido ativo, que ocupa vaga (ADR 0135). */
	private void ocuparVaga() {
		UUID pessoa = UUID.randomUUID();
		UUID inscricao = UUID.randomUUID();
		UUID responsavel = UUID.randomUUID();
		jdbc.update("insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)"
				+ " values (?, 'Pessoa Fictícia de Vagas', 'TEA', '3132345678', now(), now())", pessoa);
		jdbc.update("insert into inscricao_lista_espera (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao,"
				+ " criado_em, atualizado_em) values (?, ?, date '2024-01-01', 'CRAS', 'INSERIDO', now(), now())",
				inscricao, pessoa);
		jdbc.update("insert into responsavel (id, pessoa_id, nome_completo, criado_em, atualizado_em)"
				+ " values (?, ?, 'Responsável Fictício', now(), now())", responsavel, pessoa);
		jdbc.update("insert into insercao (id, pessoa_id, inscricao_id, data_insercao, turno, cuidador_responsavel_id,"
				+ " situacao, criado_em, atualizado_em) values (?, ?, ?, date '2024-02-01', 'MATUTINO', ?, 'ATIVA',"
				+ " now(), now())", UUID.randomUUID(), pessoa, inscricao, responsavel);
	}

	private void inserirInscricao(String situacao) {
		UUID pessoa = UUID.randomUUID();
		jdbc.update("insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)"
				+ " values (?, 'Pessoa Fictícia de Vagas', 'TEA', '3132345678', now(), now())", pessoa);
		jdbc.update("insert into inscricao_lista_espera (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao,"
				+ " criado_em, atualizado_em) values (?, ?, date '2024-01-01', 'CRAS', ?, now(), now())",
				UUID.randomUUID(), pessoa, situacao);
	}

}
