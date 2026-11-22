package br.org.cesjo.sgi.estatisticas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.org.cesjo.sgi.acesso.AutenticacaoDeTeste;
import br.org.cesjo.sgi.acesso.Perfil;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prepara dados fictícios para os testes dos relatórios pelas APIs HTTP dos outros módulos, sempre
 * como Coordenador, e lê os dashboards. O banco é compartilhado entre os testes, então os testes
 * comparam contagens antes e depois.
 */
final class DadosDeTeste {

	static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

	private final MockMvc mvc;

	private final JdbcTemplate jdbc;

	private final JsonMapper json = JsonMapper.builder().build();

	DadosDeTeste(MockMvc mvc, JdbcTemplate jdbc) {
		this.mvc = mvc;
		this.jdbc = jdbc;
	}

	/** Cadastra na fila em {@code cadastro} e devolve o corpo da inscrição criada. */
	String inscrever(String nome, LocalDate nascimento, String tipo, LocalDate cadastro) throws Exception {
		Map<String, Object> inscricao = new LinkedHashMap<>();
		inscricao.put("nomeCompleto", nome);
		inscricao.put("dataNascimento", nascimento == null ? null : nascimento.toString());
		inscricao.put("tipoDeficiencia", tipo);
		inscricao.put("telefone", "31912345678");
		inscricao.put("nomeResponsavel", "Responsável Fictício");
		inscricao.put("origemEncaminhamento", "SAUDE");
		inscricao.put("dataCadastro", cadastro.toString());
		return enviar(post("/api/lista-espera/inscricoes"), inscricao, 201);
	}

	/** Cadastra na fila dez dias antes de {@code insercao}, insere no serviço e devolve o id da inserção. */
	String inserirNoServico(String nome, LocalDate nascimento, String tipo, String turno, LocalDate insercao)
			throws Exception {
		String corpo = inscrever(nome, nascimento, tipo, insercao.minusDays(10));
		String responsavelId = jdbc.queryForObject("select id::text from responsavel where pessoa_id = ?::uuid",
				String.class, (String) JsonPath.read(corpo, "$.pessoaId"));
		Map<String, Object> nova = new LinkedHashMap<>();
		nova.put("inscricaoId", JsonPath.read(corpo, "$.id"));
		nova.put("dataInsercao", insercao.toString());
		nova.put("turno", turno);
		nova.put("cuidadorResponsavelId", responsavelId);
		return JsonPath.read(enviar(post("/api/atendidos/insercoes"), nova, 201), "$.id");
	}

	/** Completa o cadastro só com o gênero. */
	void informarGenero(String insercaoId, String nome, LocalDate nascimento, String tipo, String genero)
			throws Exception {
		Map<String, Object> cadastro = new LinkedHashMap<>();
		cadastro.put("genero", genero);
		cadastro.put("recebeBpc", false);
		cadastro.put("inscritoCadUnico", false);
		cadastro.put("recebeBolsaFamilia", false);
		Map<String, Object> edicao = new LinkedHashMap<>();
		edicao.put("nomeCompleto", nome);
		edicao.put("dataNascimento", nascimento == null ? null : nascimento.toString());
		edicao.put("tipoDeficiencia", tipo);
		edicao.put("telefone", "31912345678");
		edicao.put("cadastro", cadastro);
		enviar(put("/api/atendidos/{id}/cadastro", insercaoId), edicao, 204);
	}

	/** Registra uma violação de direitos na inserção. */
	void registrarViolacao(String insercaoId, String tipo, String ambito, LocalDate data) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("tipo", tipo);
		dados.put("ambito", ambito);
		dados.put("dataIdentificacao", data.toString());
		dados.put("descricao", "Descrição fictícia.");
		enviar(post("/api/atendidos/{id}/violacoes", insercaoId), dados, 201);
	}

	/** Desliga o usuário da inserção. */
	void desligar(String insercaoId, LocalDate data, String motivo) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("data", data.toString());
		dados.put("motivo", motivo);
		dados.put("observacao", null);
		enviar(post("/api/atendidos/{id}/desligamento", insercaoId), dados, 201);
	}

	/** Colaborador fictício ativo, gravado direto no banco, como nos testes de atividades. */
	UUID colaborador(String nome) {
		UUID id = UUID.randomUUID();
		Timestamp agora = Timestamp.from(Instant.now());
		jdbc.update("""
				insert into colaborador (id, nome, funcao, ativo, criado_em, atualizado_em)
				values (?, ?, 'PSICOLOGO', true, ?, ?)""", id, nome, agora, agora);
		return id;
	}

	/** Registra uma atividade com o usuário da inserção como participante. */
	void registrarAtividade(String tipo, LocalDate data, String insercaoId, UUID profissional) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("tipo", tipo);
		dados.put("data", data.toString());
		dados.put("titulo", "Atividade fictícia");
		dados.put("descricao", "Descrição fictícia.");
		dados.put("participantes", List.of(pessoaId(insercaoId)));
		dados.put("profissionais", List.of(profissional));
		enviar(post("/api/atividades"), dados, 201);
	}

	/** Muda a situação da inscrição, por exemplo para "saiu" com o motivo. */
	void mudarSituacao(String inscricaoId, String situacao, LocalDate data, String motivoSaida) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("situacao", situacao);
		dados.put("data", data.toString());
		dados.put("motivoSaida", motivoSaida);
		dados.put("observacao", null);
		enviar(post("/api/lista-espera/inscricoes/{id}/situacao", inscricaoId), dados, 200);
	}

	/** Registra um contato ou comunicação; {@code direcao} só para ofício, declaração e encaminhamento. */
	void registrarComunicacao(String tipo, String direcao, LocalDate data) throws Exception {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("tipo", tipo);
		dados.put("direcao", direcao);
		dados.put("data", data.toString());
		dados.put("contraparte", "Instituição Fictícia");
		dados.put("assunto", "Assunto fictício");
		dados.put("descricao", null);
		enviar(post("/api/comunicacoes"), dados, 201);
	}

	String pessoaId(String insercaoId) {
		return jdbc.queryForObject("select pessoa_id::text from insercao where id = ?::uuid", String.class, insercaoId);
	}

	/** {@code GET} como Coordenador; devolve o corpo. */
	String ler(String url, Object... variaveis) throws Exception {
		return mvc.perform(get(url, variaveis).with(comoCoordenador()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	/** Total da barra {@code chave} no gráfico {@code grafico} do corpo de um dashboard. */
	static long total(String corpo, String grafico, String chave) {
		List<Number> totais = JsonPath.read(corpo, "$." + grafico + "[?(@.chave == '" + chave + "')].total");
		return totais.isEmpty() ? 0 : totais.get(0).longValue();
	}

	String enviar(MockHttpServletRequestBuilder requisicao, Object corpo, int esperado) throws Exception {
		return mvc
			.perform(requisicao.with(csrf())
				.with(comoCoordenador())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(corpo)))
			.andExpect(status().is(esperado))
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor comoCoordenador() {
		return authentication(AutenticacaoDeTeste.de(Perfil.COORDENADOR, "estatisticas-coord-auxiliar"));
	}

}
