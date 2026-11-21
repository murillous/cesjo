package br.org.cesjo.sgi.listaespera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.arquivos.ExemplosDeArquivo;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import tools.jackson.databind.json.JsonMapper;

/** Laudo médico e comprovante de residência na lista de espera (issue #27, ADR 0055 e ADR 0056). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AnexosListaEsperaTests {

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
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "anexos-cadastro")
	void cadastroComOsDoisAnexosFicaCompleto() throws Exception {
		String laudo = enviar("laudo-ficticio.pdf", ExemplosDeArquivo.pdf());
		String comprovante = enviar("comprovante-ficticio.png", ExemplosDeArquivo.png());
		Map<String, Object> dados = dadosValidos();
		dados.put("laudoMedicoArquivoId", laudo);
		dados.put("comprovanteResidenciaArquivoId", comprovante);

		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.cadastroCompleto").value(true))
			.andExpect(jsonPath("$.documentosPendentes").isEmpty())
			.andExpect(jsonPath("$.documentos.length()").value(2))
			.andExpect(jsonPath("$.documentos[?(@.tipo == 'LAUDO_MEDICO')].nomeArquivo").value("laudo-ficticio.pdf"))
			.andExpect(jsonPath("$.documentos[?(@.tipo == 'COMPROVANTE_RESIDENCIA')].tipoMime").value("image/png"));
		verify(auditoria, org.mockito.Mockito.times(2)).registrar(
				argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("documento_pessoa")));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "anexos-incompleto")
	void semOsDoisAnexosFicaIncompletoEDepoisCompleta() throws Exception {
		String id = cadastrar(dadosValidos());
		mvc.perform(get(URL + "/{id}", id))
			.andExpect(jsonPath("$.cadastroCompleto").value(false))
			.andExpect(jsonPath("$.documentosPendentes[0]").value("LAUDO_MEDICO"))
			.andExpect(jsonPath("$.documentosPendentes[1]").value("COMPROVANTE_RESIDENCIA"));

		anexar(id, "LAUDO_MEDICO", enviar("laudo.pdf", ExemplosDeArquivo.pdf()))
			.andExpect(jsonPath("$.cadastroCompleto").value(false))
			.andExpect(jsonPath("$.documentosPendentes[0]").value("COMPROVANTE_RESIDENCIA"));
		anexar(id, "COMPROVANTE_RESIDENCIA", enviar("conta.jpg", ExemplosDeArquivo.jpeg()))
			.andExpect(jsonPath("$.cadastroCompleto").value(true));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "anexos-substitui")
	void novoEnvioSubstituiODocumentoDoMesmoTipo() throws Exception {
		String id = cadastrar(dadosValidos());
		anexar(id, "LAUDO_MEDICO", enviar("laudo-antigo.pdf", ExemplosDeArquivo.pdf()));
		clearInvocations(auditoria);

		anexar(id, "LAUDO_MEDICO", enviar("laudo-novo.pdf", ExemplosDeArquivo.pdf()))
			.andExpect(jsonPath("$.documentos.length()").value(1))
			.andExpect(jsonPath("$.documentos[0].nomeArquivo").value("laudo-novo.pdf"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidade().equals("documento_pessoa") && e.camposAlterados().contains("arquivo_id")));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "anexos-download")
	void downloadDevolveOConteudoERegistraLeituraSensivel() throws Exception {
		String id = cadastrar(dadosValidos());
		byte[] pdf = ExemplosDeArquivo.pdf();
		anexar(id, "LAUDO_MEDICO", enviar("laudo-ficticio.pdf", pdf));
		String documentoId = jdbc.queryForObject(
				"select d.id::text from documento_pessoa d join inscricao_lista_espera i on i.pessoa_id = d.pessoa_id"
						+ " where i.id = ?::uuid",
				String.class, id);
		clearInvocations(auditoria);

		mvc.perform(get(URL + "/{id}/documentos/LAUDO_MEDICO", id))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/pdf"))
			.andExpect(content().bytes(pdf))
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("laudo-ficticio.pdf")));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL
				&& e.entidade().equals("documento_pessoa") && e.entidadeId().equals(documentoId)));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL && e.entidade().equals("arquivo")));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "anexos-sem-documento")
	void downloadSemDocumentoResponde404() throws Exception {
		String id = cadastrar(dadosValidos());
		mvc.perform(get(URL + "/{id}/documentos/COMPROVANTE_RESIDENCIA", id)).andExpect(status().isNotFound());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "anexos-invalidos")
	void recusaArquivoInexistenteOuJaAnexado() throws Exception {
		String id = cadastrar(dadosValidos());
		anexar(id, "LAUDO_MEDICO", UUID.randomUUID().toString()).andExpect(status().isUnprocessableContent());

		String arquivo = enviar("laudo.pdf", ExemplosDeArquivo.pdf());
		anexar(id, "LAUDO_MEDICO", arquivo).andExpect(status().isOk());
		String outra = cadastrar(dadosValidos());
		anexar(outra, "LAUDO_MEDICO", arquivo).andExpect(status().isUnprocessableContent());
		anexar(id, "COMPROVANTE_RESIDENCIA", arquivo).andExpect(status().isUnprocessableContent());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "anexos-cadastro-invalido")
	void cadastroComArquivoInexistenteNaoGravaNada() throws Exception {
		Long antes = jdbc.queryForObject("select count(*) from inscricao_lista_espera", Long.class);
		Map<String, Object> dados = dadosValidos();
		dados.put("laudoMedicoArquivoId", UUID.randomUUID().toString());
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isUnprocessableContent());
		assertThat(jdbc.queryForObject("select count(*) from inscricao_lista_espera", Long.class)).isEqualTo(antes);
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "anexos-tecnica")
	void equipeTecnicaNaoAnexa() throws Exception {
		UUID id = UUID.randomUUID();
		mvc.perform(put(URL + "/{id}/documentos/LAUDO_MEDICO", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("arquivoId", UUID.randomUUID().toString())))).andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "anexos-consulta")
	void consultaNaoBaixaNemAnexa() throws Exception {
		UUID id = UUID.randomUUID();
		mvc.perform(get(URL + "/{id}/documentos/LAUDO_MEDICO", id)).andExpect(status().isForbidden());
		mvc.perform(put(URL + "/{id}/documentos/LAUDO_MEDICO", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("arquivoId", UUID.randomUUID().toString())))).andExpect(status().isForbidden());
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL));
	}

	private String enviar(String nome, byte[] conteudo) throws Exception {
		String corpo = mvc
			.perform(multipart("/api/arquivos").file(new MockMultipartFile("arquivo", nome, null, conteudo)).with(csrf()))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private String cadastrar(Map<String, Object> dados) throws Exception {
		String corpo = mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dados)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private org.springframework.test.web.servlet.ResultActions anexar(String inscricaoId, String tipo, String arquivoId)
			throws Exception {
		return mvc.perform(put(URL + "/{id}/documentos/{tipo}", inscricaoId, tipo).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("arquivoId", arquivoId))));
	}

	private static Map<String, Object> dadosValidos() {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("nomeCompleto", "Pessoa Fictícia com Anexos");
		dados.put("tipoDeficiencia", "FISICA");
		dados.put("telefone", "3132345678");
		dados.put("nomeResponsavel", "Responsável Fictício");
		dados.put("origemEncaminhamento", "SAUDE");
		dados.put("dataCadastro", "2026-11-03");
		return dados;
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
