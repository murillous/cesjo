package br.org.cesjo.sgi.atendidos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.acesso.AutenticacaoDeTeste;
import br.org.cesjo.sgi.acesso.Perfil;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prepara usuários atendidos fictícios pelas APIs, sempre como Coordenador, qualquer que seja a conta
 * do teste.
 */
final class AtendidosDeTeste {

	static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

	private final MockMvc mvc;

	private final JdbcTemplate jdbc;

	private final JsonMapper json = JsonMapper.builder().build();

	AtendidosDeTeste(MockMvc mvc, JdbcTemplate jdbc) {
		this.mvc = mvc;
		this.jdbc = jdbc;
	}

	/** Cadastra na fila (dez dias atrás) e insere no serviço (ontem); devolve o id da inserção. */
	String inserirNoServico(String nome, LocalDate nascimento, String tipo, String turno) throws Exception {
		Map<String, Object> inscricao = new LinkedHashMap<>();
		inscricao.put("nomeCompleto", nome);
		inscricao.put("dataNascimento", nascimento == null ? null : nascimento.toString());
		inscricao.put("tipoDeficiencia", tipo);
		inscricao.put("telefone", "31912345678");
		inscricao.put("nomeResponsavel", "Responsável Fictício");
		inscricao.put("origemEncaminhamento", "SAUDE");
		inscricao.put("dataCadastro", HOJE.minusDays(10).toString());
		String corpo = criar("/api/lista-espera/inscricoes", inscricao);
		String responsavelId = jdbc.queryForObject("select id::text from responsavel where pessoa_id = ?::uuid",
				String.class, (String) JsonPath.read(corpo, "$.pessoaId"));

		Map<String, Object> insercao = new LinkedHashMap<>();
		insercao.put("inscricaoId", JsonPath.read(corpo, "$.id"));
		insercao.put("dataInsercao", HOJE.minusDays(1).toString());
		insercao.put("turno", turno);
		insercao.put("cuidadorResponsavelId", responsavelId);
		return JsonPath.read(criar("/api/atendidos/insercoes", insercao), "$.id");
	}

	/** {@code POST} como Coordenador, esperando 201; devolve o corpo. */
	String criar(String url, Object corpo) throws Exception {
		return mvc
			.perform(post(url).with(csrf())
				.with(authentication(AutenticacaoDeTeste.de(Perfil.COORDENADOR, "atendidos-coord-auxiliar")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(corpo)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

}
