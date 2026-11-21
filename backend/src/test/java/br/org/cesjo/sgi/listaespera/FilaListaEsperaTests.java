package br.org.cesjo.sgi.listaespera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.arquivos.ExemplosDeArquivo;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fila da lista de espera com posição, filtros, busca e dados básicos (issue #28, ADR 0057 a ADR 0059).
 * Os testes compartilham o banco com outros, então cada um usa um sobrenome próprio na busca e confere
 * a ordem relativa das posições.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FilaListaEsperaTests {

	private static final String URL = "/api/lista-espera/inscricoes";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	private final JsonMapper json = JsonMapper.builder().build();

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "fila-ordem")
	void listaEmOrdemDeDataDeCadastroComDesempatePeloCadastro() throws Exception {
		String terceira = cadastrar("Carla Ordemfila", "2020-03-10", "TEA", "CRAS");
		String primeira = cadastrar("Ana Ordemfila", "2020-03-01", "FISICA", "SAUDE");
		String segunda = cadastrar("Bruno Ordemfila", "2020-03-10", "TEA", "CRAS");

		String corpo = mvc.perform(get(URL).param("nome", "ordemfila"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.totalElements").value(3))
			.andExpect(jsonPath("$.content[0].nomeCompleto").value("Ana Ordemfila"))
			.andExpect(jsonPath("$.content[0].tipoDeficiencia").value("FISICA"))
			.andExpect(jsonPath("$.content[0].origemEncaminhamento").value("SAUDE"))
			.andExpect(jsonPath("$.content[0].cadastroCompleto").value(false))
			.andExpect(jsonPath("$.content[0].situacao").value("AGUARDANDO"))
			.andExpect(jsonPath("$.content[0].dataCadastro").value("2020-03-01"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		List<String> ids = JsonPath.read(corpo, "$.content[*].id");
		List<Integer> posicoes = JsonPath.read(corpo, "$.content[*].posicao");
		assertThat(ids).containsExactly(primeira, terceira, segunda);
		assertThat(posicoes).isSortedAccordingTo(Integer::compare).doesNotHaveDuplicates();
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "fila-filtros")
	void filtrosNaoMudamAPosicao() throws Exception {
		cadastrar("Daniel Filtrofila", "2019-05-01", "INTELECTUAL", "CREAS");
		cadastrar("Elisa Filtrofila", "2019-05-02", "T21", "EDUCACAO");

		String todos = mvc.perform(get(URL).param("nome", "FILTROFILA"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		Integer posicaoElisa = JsonPath.<List<Integer>>read(todos, "$.content[?(@.nomeCompleto == 'Elisa Filtrofila')].posicao")
			.getFirst();

		mvc.perform(get(URL).param("nome", "filtrofila").param("origem", "EDUCACAO"))
			.andExpect(jsonPath("$.page.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].nomeCompleto").value("Elisa Filtrofila"))
			.andExpect(jsonPath("$.content[0].posicao").value(posicaoElisa));
		mvc.perform(get(URL).param("nome", "filtrofila").param("tipoDeficiencia", "INTELECTUAL"))
			.andExpect(jsonPath("$.page.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].nomeCompleto").value("Daniel Filtrofila"));
		mvc.perform(get(URL).param("nome", "filtrofila").param("situacao", "AGUARDANDO"))
			.andExpect(jsonPath("$.page.totalElements").value(2));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "fila-paginas")
	void paginaEMostraCadastroCompleto() throws Exception {
		for (int i = 1; i <= 3; i++) {
			cadastrar("Pessoa " + i + " Paginafila", "2018-01-0" + i, "SENSORIAL", "CRAS");
		}
		cadastrarCompleto("Pessoa 4 Paginafila", "2018-01-04");

		mvc.perform(get(URL).param("nome", "paginafila").param("size", "2").param("page", "1"))
			.andExpect(jsonPath("$.page.totalElements").value(4))
			.andExpect(jsonPath("$.page.totalPages").value(2))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].nomeCompleto").value("Pessoa 3 Paginafila"))
			.andExpect(jsonPath("$.content[1].nomeCompleto").value("Pessoa 4 Paginafila"))
			.andExpect(jsonPath("$.content[1].cadastroCompleto").value(true));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "fila-busca-curinga")
	void buscaTrataCuringaComoTexto() throws Exception {
		cadastrar("Fulano Curingafila", "2017-01-01", "TEA", "CRAS");
		mvc.perform(get(URL).param("nome", "curinga%fila")).andExpect(jsonPath("$.page.totalElements").value(0));
		mvc.perform(get(URL).param("nome", "  curingafila ")).andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "fila-consulta")
	void consultaVeSoDadosBasicos() throws Exception {
		inserirDireto("Gilda Basicofila");
		String corpo = mvc.perform(get(URL).param("nome", "basicofila"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.totalElements").value(1))
			.andReturn()
			.getResponse()
			.getContentAsString();
		List<Map<String, Object>> itens = JsonPath.read(corpo, "$.content");
		assertThat(itens).hasSize(1);
		for (Map<String, Object> item : itens) {
			assertThat(item).containsKeys("id", "nomeCompleto", "situacao", "dataCadastro", "posicao")
				.doesNotContainKeys("tipoDeficiencia", "origemEncaminhamento", "cadastroCompleto", "telefone",
						"nomeResponsavel", "documentos");
		}
		mvc.perform(get(URL).param("tipoDeficiencia", "TEA")).andExpect(status().isForbidden());
		mvc.perform(get(URL).param("origem", "CRAS")).andExpect(status().isForbidden());
		mvc.perform(get(URL).param("nome", "fulano").param("situacao", "AGUARDANDO")).andExpect(status().isOk());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "fila-filtro-invalido")
	void filtroComValorDesconhecidoResponde400() throws Exception {
		mvc.perform(get(URL).param("situacao", "INEXISTENTE")).andExpect(status().isBadRequest());
	}

	/** O perfil Consulta não cadastra; o candidato entra direto no banco. */
	private void inserirDireto(String nome) {
		UUID pessoa = UUID.randomUUID();
		jdbc.update("insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)"
				+ " values (?, ?, 'TEA', '3132345678', now(), now())", pessoa, nome);
		jdbc.update("insert into inscricao_lista_espera (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao,"
				+ " criado_em, atualizado_em) values (?, ?, date '2016-01-01', 'CRAS', 'AGUARDANDO', now(), now())",
				UUID.randomUUID(), pessoa);
	}

	private String cadastrar(String nome, String dataCadastro, String tipo, String origem) throws Exception {
		return cadastrar(dados(nome, dataCadastro, tipo, origem));
	}

	private void cadastrarCompleto(String nome, String dataCadastro) throws Exception {
		Map<String, Object> dados = dados(nome, dataCadastro, "SENSORIAL", "CRAS");
		dados.put("laudoMedicoArquivoId", enviar(ExemplosDeArquivo.pdf()));
		dados.put("comprovanteResidenciaArquivoId", enviar(ExemplosDeArquivo.png()));
		cadastrar(dados);
	}

	private String cadastrar(Map<String, Object> dados) throws Exception {
		String corpo = mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private String enviar(byte[] conteudo) throws Exception {
		String corpo = mvc
			.perform(multipart("/api/arquivos")
				.file(new MockMultipartFile("arquivo", "doc-" + UUID.randomUUID(), null, conteudo))
				.with(csrf()))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private static Map<String, Object> dados(String nome, String dataCadastro, String tipo, String origem) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("nomeCompleto", nome);
		dados.put("tipoDeficiencia", tipo);
		dados.put("telefone", "3132345678");
		dados.put("nomeResponsavel", "Responsável Fictício");
		dados.put("origemEncaminhamento", origem);
		dados.put("dataCadastro", dataCadastro);
		return dados;
	}

}
