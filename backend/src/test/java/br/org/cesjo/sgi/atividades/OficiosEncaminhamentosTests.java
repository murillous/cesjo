package br.org.cesjo.sgi.atividades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.arquivos.Arquivos;
import br.org.cesjo.sgi.arquivos.ExemplosDeArquivo;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ofícios, declarações e encaminhamentos (issue #49, RF011): direção enviada ou recebida, anexo opcional,
 * listagem por instituição e período, permissões e contagem por direção. Só dados fictícios. Os testes
 * que contam ou filtram usam datas antigas e nomes próprios, para não somar os registros dos outros.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OficiosEncaminhamentosTests {

	private static final String URL = "/api/comunicacoes";

	private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

	@Autowired
	MockMvc mvc;

	@Autowired
	Comunicacoes comunicacoes;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "oficio-tecnica")
	void registraOficioExpedidoSemAnexo() throws Exception {
		registrar(dados("OFICIO", "ENVIADA", HOJE, "CREAS Fictício", "Ofício 12/2026: pedido de visita"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tipo").value("OFICIO"))
			.andExpect(jsonPath("$.direcao").value("ENVIADA"))
			.andExpect(jsonPath("$.anexo").doesNotExist());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "oficio-direcao")
	void direcaoSoNosTiposQueAPedem() throws Exception {
		registrar(dados("DECLARACAO", null, HOJE, "Escola Fictícia", "Declaração de frequência"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("direcaoDoTipo")));
		registrar(dados("CONTATO_INSTITUCIONAL", "RECEBIDA", HOJE, "UBS Fictícia", "Consulta"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("direcaoDoTipo")));
		registrar(dados("ENCAMINHAMENTO", "RECEBIDA", HOJE, "CRAS Fictício", "Encaminhamento de usuário"))
			.andExpect(status().isCreated());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "oficio-anexo")
	void anexaBaixaSubstituiERemoveOAnexo() throws Exception {
		String id = criar(dados("OFICIO", "RECEBIDA", HOJE, "Fórum Fictício", "Ofício recebido"));
		clearInvocations(auditoria);

		anexar(id, "oficio-ficticio.pdf", ExemplosDeArquivo.pdf()).andExpect(status().isOk())
			.andExpect(jsonPath("$.anexo.nomeOriginal").value("oficio-ficticio.pdf"))
			.andExpect(jsonPath("$.anexo.tipo").value("PDF"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidade().equals(Comunicacoes.ENTIDADE) && e.camposAlterados().equals(Set.of("arquivo_id"))));
		mvc.perform(get(URL).param("contraparte", "fórum fictício"))
			.andExpect(jsonPath("$.content[0].temAnexo").value(true));

		clearInvocations(auditoria);
		mvc.perform(get(URL + "/{id}/anexo", id))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/pdf"))
			.andExpect(content().bytes(ExemplosDeArquivo.pdf()));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL
				&& e.entidade().equals(Arquivos.ENTIDADE)));

		anexar(id, "foto-ficticia.png", ExemplosDeArquivo.png()).andExpect(status().isOk())
			.andExpect(jsonPath("$.anexo.tipo").value("PNG"));
		anexar(id, "programa.exe", "MZ fictício".getBytes()).andExpect(status().isUnprocessableContent());

		mvc.perform(delete(URL + "/{id}/anexo", id).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.anexo").doesNotExist());
		mvc.perform(get(URL + "/{id}/anexo", id)).andExpect(status().isNotFound());

		clearInvocations(auditoria);
		mvc.perform(delete(URL + "/{id}/anexo", id).with(csrf())).andExpect(status().isOk());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "oficio-lista")
	void listaPorInstituicaoEPeriodo() throws Exception {
		LocalDate dia = LocalDate.of(2004, 8, 10);
		criar(dados("OFICIO", "ENVIADA", dia, "Instituto Lírio Fictício", "Ofício de agosto"));
		criar(dados("ENCAMINHAMENTO", "RECEBIDA", dia.plusDays(2), "instituto lírio fictício", "Encaminhamento"));
		criar(dados("OFICIO", "ENVIADA", dia.plusMonths(1), "Instituto Lírio Fictício", "Fora do período"));
		criar(dados("OFICIO", "ENVIADA", dia, "Outra Casa Fictícia", "Outra instituição"));

		mvc.perform(get(URL).param("contraparte", "LÍRIO")
			.param("inicio", "2004-08-01")
			.param("fim", "2004-08-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.totalElements").value(2))
			.andExpect(jsonPath("$.content[0].assunto").value("Encaminhamento"))
			.andExpect(jsonPath("$.content[1].direcao").value("ENVIADA"));
		mvc.perform(get(URL).param("contraparte", "lírio").param("tipo", "OFICIO").param("direcao", "ENVIADA"))
			.andExpect(jsonPath("$.page.totalElements").value(2));
		mvc.perform(get(URL).param("contraparte", "%")).andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "oficio-contagem")
	void contaPorDirecaoNoPeriodo() throws Exception {
		LocalDate dia = LocalDate.of(2005, 2, 1);
		criar(dados("ENCAMINHAMENTO", "ENVIADA", dia, "CAPS Fictício", "Assunto"));
		criar(dados("ENCAMINHAMENTO", "ENVIADA", dia.plusDays(27), "CRAS Fictício", "Assunto"));
		criar(dados("ENCAMINHAMENTO", "RECEBIDA", dia.plusMonths(1), "CREAS Fictício", "Fora do mês"));
		criar(dados("OFICIO", "RECEBIDA", dia.plusDays(3), "Fórum Fictício", "Assunto"));

		LocalDate fim = dia.withDayOfMonth(28);
		assertThat(comunicacoes.contarPorDirecao(TipoComunicacao.ENCAMINHAMENTO, dia, fim))
			.containsEntry(DirecaoComunicacao.ENVIADA, 2L)
			.containsEntry(DirecaoComunicacao.RECEBIDA, 0L);
		assertThat(comunicacoes.contarPorTipo(dia, fim)).containsEntry(TipoComunicacao.ENCAMINHAMENTO, 2L)
			.containsEntry(TipoComunicacao.OFICIO, 1L)
			.containsEntry(TipoComunicacao.DECLARACAO, 0L);
		assertThatThrownBy(() -> comunicacoes.contarPorDirecao(TipoComunicacao.CONTATO_INSTITUCIONAL, dia, fim))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "oficio-consulta")
	void consultaNaoAnexaNemBaixa() throws Exception {
		UUID id = UUID.randomUUID();
		anexar(id.toString(), "oficio.pdf", ExemplosDeArquivo.pdf()).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}/anexo", id)).andExpect(status().isForbidden());
		mvc.perform(delete(URL + "/{id}/anexo", id).with(csrf())).andExpect(status().isForbidden());
	}

	private String criar(Map<String, Object> dados) throws Exception {
		return JsonPath.read(registrar(dados).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
				"$.id");
	}

	private ResultActions registrar(Map<String, Object> dados) throws Exception {
		return mvc.perform(
				post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados)));
	}

	private ResultActions anexar(String id, String nome, byte[] conteudo) throws Exception {
		return mvc.perform(multipart(URL + "/{id}/anexo", id)
			.file(new MockMultipartFile("arquivo", nome, MediaType.APPLICATION_OCTET_STREAM_VALUE, conteudo))
			.with(csrf()));
	}

	private static Map<String, Object> dados(String tipo, String direcao, LocalDate data, String contraparte,
			String assunto) {
		Map<String, Object> dados = new LinkedHashMap<>();
		dados.put("tipo", tipo);
		dados.put("direcao", direcao);
		dados.put("data", data.toString());
		dados.put("contraparte", contraparte);
		dados.put("assunto", assunto);
		return dados;
	}

}
