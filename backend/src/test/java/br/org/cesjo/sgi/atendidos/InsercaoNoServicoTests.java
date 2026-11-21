package br.org.cesjo.sgi.atendidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
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
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
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
 * Inserção de candidato da fila no serviço (issue #39): a inscrição sai da fila como inserida, a
 * inserção ativa ocupa vaga, o cuidador vem dos responsáveis da pessoa e tudo é auditado. Só dados
 * fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class InsercaoNoServicoTests {

	private static final String URL = "/api/atendidos/insercoes";

	private static final String COORDENADOR = "atendidos-coordenador";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	UsuariosAtendidos usuariosAtendidos;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@AfterEach
	void restaurarAuditoria() {
		reset(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void insereComOResponsavelDoCadastroETiraDaFila() throws Exception {
		Inscrito inscrito = inscrever();
		long ativosAntes = usuariosAtendidos.contarAtivos();
		long matutinoAntes = usuariosAtendidos.contarAtivosPorTurno().get(Turno.MATUTINO);

		mvc.perform(get("/api/atendidos/candidatos/{id}", inscrito.inscricaoId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nomeCompleto").value("Pessoa Fictícia Atendida"))
			.andExpect(jsonPath("$.situacao").value("AGUARDANDO"))
			.andExpect(jsonPath("$.responsaveis[0].nomeCompleto").value("Responsável Fictício"))
			.andExpect(jsonPath("$.responsaveis[0].principal").value(true));

		String corpo = inserir(dados(inscrito.inscricaoId(), inscrito.responsavelId(), null)).andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/atendidos/")))
			.andExpect(jsonPath("$.pessoaId").value(inscrito.pessoaId()))
			.andExpect(jsonPath("$.turno").value("MATUTINO"))
			.andExpect(jsonPath("$.dataInsercao").value("2026-11-10"))
			.andExpect(jsonPath("$.nomeCuidador").value("Responsável Fictício"))
			.andExpect(jsonPath("$.situacao").value("ATIVA"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String insercaoId = JsonPath.read(corpo, "$.id");

		mvc.perform(get("/api/lista-espera/inscricoes/{id}", inscrito.inscricaoId()))
			.andExpect(jsonPath("$.situacao").value("INSERIDO"));
		assertThat(usuariosAtendidos.contarAtivos()).as("vagas disponíveis diminuem").isEqualTo(ativosAntes + 1);
		assertThat(usuariosAtendidos.contarAtivosPorTurno().get(Turno.MATUTINO)).isEqualTo(matutinoAntes + 1);
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("insercao")
				&& e.entidadeId().equals(insercaoId)));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidade().equals("inscricao_lista_espera") && e.entidadeId().equals(inscrito.inscricaoId())
				&& e.camposAlterados().contains("situacao")));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void cadastraNovoCuidadorQuandoInformadoPeloNome() throws Exception {
		Inscrito inscrito = inscrever();
		clearInvocations(auditoria);

		inserir(dados(inscrito.inscricaoId(), null, "  Cuidadora Fictícia ")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.nomeCuidador").value("Cuidadora Fictícia"));

		assertThat(jdbc.queryForObject("select count(*) from responsavel where pessoa_id = ?::uuid", Long.class,
				inscrito.pessoaId()))
			.isEqualTo(2);
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("responsavel")));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void naoInsereDuasVezesAMesmaInscricao() throws Exception {
		Inscrito inscrito = inscrever();
		inserir(dados(inscrito.inscricaoId(), inscrito.responsavelId(), null)).andExpect(status().isCreated());

		inserir(dados(inscrito.inscricaoId(), inscrito.responsavelId(), null)).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("aguardando")));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void recusaRegrasDeNegocio() throws Exception {
		Inscrito inscrito = inscrever();
		Inscrito outro = inscrever();
		Map<String, Object> antesDoCadastro = dados(inscrito.inscricaoId(), inscrito.responsavelId(), null);
		antesDoCadastro.put("dataInsercao", "2026-11-01");

		inserir(antesDoCadastro).andExpect(status().isUnprocessableContent());
		inserir(dados(inscrito.inscricaoId(), outro.responsavelId(), null)).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("O cuidador escolhido não é responsável por esta pessoa."));
		inserir(dados(UUID.randomUUID().toString(), inscrito.responsavelId(), null)).andExpect(status().isNotFound());

		assertThat(situacao(inscrito.inscricaoId())).isEqualTo("AGUARDANDO");
		verify(auditoria, never()).registrar(argThat(e -> e.entidade().equals("insercao")));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void recusaDadosInvalidos() throws Exception {
		Inscrito inscrito = inscrever();
		Map<String, Object> dados = dados(inscrito.inscricaoId(), inscrito.responsavelId(), "Outro Cuidador");
		dados.put("dataInsercao", LocalDate.now().plusDays(2).toString());
		dados.remove("turno");

		inserir(dados).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("dataInsercao")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("turno")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("cuidador")));
		inserir(dados(inscrito.inscricaoId(), null, " ")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("cuidador")));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = COORDENADOR)
	void falhaDepoisDeTirarDaFilaDesfazTudo() throws Exception {
		Inscrito inscrito = inscrever();
		long ativosAntes = usuariosAtendidos.contarAtivos();
		doThrow(new IllegalStateException("auditoria fora do ar")).when(auditoria)
			.registrar(argThat(e -> e != null && e.entidade().equals("insercao")));

		inserir(dados(inscrito.inscricaoId(), inscrito.responsavelId(), null)).andExpect(status().isInternalServerError());

		assertThat(situacao(inscrito.inscricaoId())).isEqualTo("AGUARDANDO");
		assertThat(usuariosAtendidos.contarAtivos()).isEqualTo(ativosAntes);
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "atendidos-tecnica")
	void equipeTecnicaNaoInsere() throws Exception {
		exigirProibido();
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "atendidos-administrativo")
	void administrativoNaoInsere() throws Exception {
		exigirProibido();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "atendidos-consulta")
	void consultaNaoInsere() throws Exception {
		exigirProibido();
	}

	private void exigirProibido() throws Exception {
		UUID id = UUID.randomUUID();
		inserir(dados(id.toString(), id.toString(), null)).andExpect(status().isForbidden());
		mvc.perform(get("/api/atendidos/candidatos/{id}", id)).andExpect(status().isForbidden());
	}

	private ResultActions inserir(Map<String, Object> dados) throws Exception {
		return mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados)));
	}

	private static Map<String, Object> dados(String inscricaoId, String cuidadorId, String nomeNovoCuidador) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("inscricaoId", inscricaoId);
		dados.put("dataInsercao", "2026-11-10");
		dados.put("turno", "MATUTINO");
		dados.put("cuidadorResponsavelId", cuidadorId);
		dados.put("nomeNovoCuidador", nomeNovoCuidador);
		return dados;
	}

	private String situacao(String inscricaoId) {
		return jdbc.queryForObject("select situacao from inscricao_lista_espera where id = ?::uuid", String.class,
				inscricaoId);
	}

	/** Cadastra um candidato na fila pela API da lista de espera. */
	private Inscrito inscrever() throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("nomeCompleto", "Pessoa Fictícia Atendida");
		dados.put("dataNascimento", "1995-03-20");
		dados.put("tipoDeficiencia", "INTELECTUAL");
		dados.put("telefone", "31912345678");
		dados.put("nomeResponsavel", "Responsável Fictício");
		dados.put("origemEncaminhamento", "CRAS");
		dados.put("dataCadastro", "2026-11-02");
		String corpo = mvc
			.perform(post("/api/lista-espera/inscricoes").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(dados)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String pessoaId = JsonPath.read(corpo, "$.pessoaId");
		String responsavelId = jdbc.queryForObject("select id::text from responsavel where pessoa_id = ?::uuid",
				String.class, pessoaId);
		return new Inscrito(JsonPath.read(corpo, "$.id"), pessoaId, responsavelId);
	}

	private record Inscrito(String inscricaoId, String pessoaId, String responsavelId) {
	}

}
