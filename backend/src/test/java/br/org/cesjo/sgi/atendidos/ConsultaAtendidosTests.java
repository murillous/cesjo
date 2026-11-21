package br.org.cesjo.sgi.atendidos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.AutenticacaoDeTeste;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import tools.jackson.databind.json.JsonMapper;

/**
 * Listagem e ficha dos usuários atendidos (issue #41): filtros, visão completa para quem tem
 * {@code ATENDIDOS_LER} e só dados básicos para Administrativo e Consulta. Cada teste usa um prefixo de
 * nome próprio, porque o banco é compartilhado. Só dados fictícios.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ConsultaAtendidosTests {

	private static final String URL = "/api/atendidos";

	private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	private final JsonMapper json = JsonMapper.builder().build();

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "atendidos-lista-coord")
	void listaCompletaComFiltrosEmOrdemDeNome() throws Exception {
		String prefixo = prefixo();
		inserirNoServico(prefixo + " Bruna", HOJE.minusYears(20), "TEA", "MATUTINO");
		inserirNoServico(prefixo + " Ana", HOJE.minusYears(40), "FISICA", "VESPERTINO");
		inserirNoServico(prefixo + " Carla", null, "TEA", "INTEGRAL");

		listar("nome", prefixo).andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(3)))
			.andExpect(jsonPath("$.page.totalElements").value(3))
			.andExpect(jsonPath("$.content[0].nomeCompleto").value(prefixo + " Ana"))
			.andExpect(jsonPath("$.content[0].completo").value(true))
			.andExpect(jsonPath("$.content[0].tipoDeficiencia").value("FISICA"))
			.andExpect(jsonPath("$.content[0].idade").value(40))
			.andExpect(jsonPath("$.content[0].faixaEtaria").value("ADULTO"))
			.andExpect(jsonPath("$.content[1].faixaEtaria").value("JOVEM"))
			.andExpect(jsonPath("$.content[2].faixaEtaria").doesNotExist());

		listar("nome", prefixo, "turno", "MATUTINO").andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].nomeCompleto").value(prefixo + " Bruna"));
		listar("nome", prefixo, "tipoDeficiencia", "TEA").andExpect(jsonPath("$.content", hasSize(2)));
		listar("nome", prefixo, "faixaEtaria", "JOVEM").andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].nomeCompleto").value(prefixo + " Bruna"));
		listar("nome", prefixo, "faixaEtaria", "ADULTO").andExpect(jsonPath("$.content", hasSize(1)));
		listar("nome", prefixo, "situacao", "ATIVA").andExpect(jsonPath("$.content", hasSize(3)));
		listar("nome", prefixo, "situacao", "DESLIGADA").andExpect(jsonPath("$.content", hasSize(0)));
		listar("nome", prefixo.toUpperCase(), "size", "2", "page", "1").andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].nomeCompleto").value(prefixo + " Carla"));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "atendidos-lista-curinga")
	void buscaPorNomeNaoTrataPorcentagemComoCuringa() throws Exception {
		String prefixo = prefixo();
		inserirNoServico(prefixo + " Dora", HOJE.minusYears(25), "TEA", "MATUTINO");

		listar("nome", prefixo.substring(0, 6) + "%").andExpect(jsonPath("$.content", hasSize(0)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "atendidos-ficha-coord")
	void fichaCompletaReuneInsercaoPessoaCuidadorEInscricao() throws Exception {
		String id = inserirNoServico(prefixo() + " Elisa", HOJE.minusYears(31), "T21", "INTEGRAL");

		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completo").value(true))
			.andExpect(jsonPath("$.turno").value("INTEGRAL"))
			.andExpect(jsonPath("$.situacao").value("ATIVA"))
			.andExpect(jsonPath("$.tipoDeficiencia").value("T21"))
			.andExpect(jsonPath("$.faixaEtaria").value("ADULTO"))
			.andExpect(jsonPath("$.telefone").value("31912345678"))
			.andExpect(jsonPath("$.nomeCuidador").value("Responsável Fictício"))
			.andExpect(jsonPath("$.inscricao.origemEncaminhamento").value("SAUDE"))
			.andExpect(jsonPath("$.inscricao.dataCadastro").value(HOJE.minusDays(10).toString()));
		mvc.perform(get(URL + "/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "atendidos-lista-consulta")
	void consultaVeSoDadosBasicos() throws Exception {
		String prefixo = prefixo();
		String id = inserirNoServico(prefixo + " Fabi", HOJE.minusYears(22), "TEA", "MATUTINO");

		listar("nome", prefixo, "turno", "MATUTINO").andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].completo").value(false))
			.andExpect(jsonPath("$.content[0].nomeCompleto").value(prefixo + " Fabi"))
			.andExpect(jsonPath("$.content[0].turno").value("MATUTINO"))
			.andExpect(jsonPath("$.content[0].situacao").value("ATIVA"))
			.andExpect(jsonPath("$.content[0].dataInsercao").exists())
			.andExpect(jsonPath("$.content[0].tipoDeficiencia").doesNotExist())
			.andExpect(jsonPath("$.content[0].dataNascimento").doesNotExist())
			.andExpect(jsonPath("$.content[0].idade").doesNotExist())
			.andExpect(jsonPath("$.content[0].pessoaId").doesNotExist());
		listar("nome", prefixo, "tipoDeficiencia", "TEA").andExpect(status().isForbidden());
		listar("nome", prefixo, "faixaEtaria", "JOVEM").andExpect(status().isForbidden());

		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completo").value(false))
			.andExpect(jsonPath("$.nomeCompleto").value(prefixo + " Fabi"))
			.andExpect(jsonPath("$.telefone").doesNotExist())
			.andExpect(jsonPath("$.nomeCuidador").doesNotExist())
			.andExpect(jsonPath("$.inscricao").doesNotExist());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "atendidos-lista-admin")
	void administrativoVeSoDadosBasicos() throws Exception {
		listar().andExpect(status().isOk());
		listar("tipoDeficiencia", "TEA").andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "atendidos-lista-tecnica")
	void equipeTecnicaVeAFichaCompleta() throws Exception {
		listar("tipoDeficiencia", "TEA").andExpect(status().isOk());
	}

	@Test
	void semLoginNaoLista() throws Exception {
		listar().andExpect(status().isUnauthorized());
	}

	private ResultActions listar(String... parametros) throws Exception {
		var requisicao = get(URL);
		for (int i = 0; i < parametros.length; i += 2) {
			requisicao.param(parametros[i], parametros[i + 1]);
		}
		return mvc.perform(requisicao);
	}

	/** Nome único, para os testes não verem os registros uns dos outros. */
	private static String prefixo() {
		return "Teste" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * Cadastra na fila e insere no serviço pelas APIs, sempre como Coordenador, qualquer que seja a conta
	 * do teste; devolve o id da inserção.
	 */
	private String inserirNoServico(String nome, LocalDate nascimento, String tipo, String turno) throws Exception {
		Map<String, Object> inscricao = new LinkedHashMap<>();
		inscricao.put("nomeCompleto", nome);
		inscricao.put("dataNascimento", nascimento == null ? null : nascimento.toString());
		inscricao.put("tipoDeficiencia", tipo);
		inscricao.put("telefone", "31912345678");
		inscricao.put("nomeResponsavel", "Responsável Fictício");
		inscricao.put("origemEncaminhamento", "SAUDE");
		inscricao.put("dataCadastro", HOJE.minusDays(10).toString());
		String corpo = criar("/api/lista-espera/inscricoes", inscricao);
		String inscricaoId = JsonPath.read(corpo, "$.id");
		String responsavelId = jdbc.queryForObject("select id::text from responsavel where pessoa_id = ?::uuid",
				String.class, (String) JsonPath.read(corpo, "$.pessoaId"));

		Map<String, Object> insercao = new LinkedHashMap<>();
		insercao.put("inscricaoId", inscricaoId);
		insercao.put("dataInsercao", HOJE.minusDays(1).toString());
		insercao.put("turno", turno);
		insercao.put("cuidadorResponsavelId", responsavelId);
		return JsonPath.read(criar("/api/atendidos/insercoes", insercao), "$.id");
	}

	private String criar(String url, Object corpo) throws Exception {
		return mvc
			.perform(post(url).with(csrf())
				.with(authentication(AutenticacaoDeTeste.de(Perfil.COORDENADOR, "atendidos-coord-auxiliar")))
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

}
