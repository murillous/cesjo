package br.org.cesjo.sgi.atividades;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import tools.jackson.databind.json.JsonMapper;

/** Listagem de atividades por período (issue #47): filtros por tipo, período, profissional e usuário. */
@Import({ TestcontainersConfiguration.class, DadosDeAtividade.class })
@SpringBootTest
@AutoConfigureMockMvc
class ListagemAtividadesTests {

	private static final String URL = "/api/atividades";

	@Autowired
	MockMvc mvc;

	@Autowired
	DadosDeAtividade dados;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "listagem-tecnica-filtros")
	void filtraPorTipoPeriodoProfissionalEUsuario() throws Exception {
		// Datas distantes para não misturar com as atividades dos outros testes no banco compartilhado.
		LocalDate dia = LocalDate.of(2005, 3, 10);
		UUID ana = dados.usuario("Ana Listagem Fictícia", "ATIVA");
		UUID bruno = dados.usuario("Bruno Listagem Fictício", "ATIVA");
		UUID psicologa = dados.colaborador("Psicóloga Listagem", true);
		UUID assistente = dados.colaborador("Assistente Listagem", true);
		String oficina = registrar("COM_USUARIOS", dia, List.of(ana, bruno), List.of(psicologa));
		String reuniao = registrar("COM_FAMILIAS", dia.plusDays(5), List.of(bruno), List.of(assistente));
		String estudo = registrar("ESTUDO_DE_CASO", dia.plusDays(20), List.of(ana), List.of(psicologa, assistente));

		mvc.perform(get(URL).param("de", dia.toString()).param("ate", dia.plusDays(30).toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id").value(contains(estudo, reuniao, oficina)))
			.andExpect(jsonPath("$.page.totalElements").value(3));
		mvc.perform(get(URL).param("de", dia.plusDays(1).toString()).param("ate", dia.plusDays(10).toString()))
			.andExpect(jsonPath("$.content[*].id").value(contains(reuniao)));
		mvc.perform(get(URL).param("de", dia.toString()).param("ate", dia.plusDays(30).toString()).param("tipo", "ESTUDO_DE_CASO"))
			.andExpect(jsonPath("$.content[*].id").value(contains(estudo)));
		mvc.perform(get(URL).param("profissionalId", assistente.toString()))
			.andExpect(jsonPath("$.content[*].id").value(contains(estudo, reuniao)));
		mvc.perform(get(URL).param("pessoaId", ana.toString()))
			.andExpect(jsonPath("$.content[*].id").value(contains(estudo, oficina)))
			.andExpect(jsonPath("$.content[0].descricao").doesNotExist());
		mvc.perform(get(URL).param("pessoaId", bruno.toString()).param("profissionalId", psicologa.toString()))
			.andExpect(jsonPath("$.content[*].id").value(contains(oficina)));
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "listagem-coord-periodo")
	void recusaPeriodoInvertido() throws Exception {
		mvc.perform(get(URL).param("de", "2026-02-10").param("ate", "2026-02-01"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("O fim do período não pode ser anterior ao início."));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "listagem-coord-opcoes")
	void ofereceNosFiltrosQuemTemAtividadeMesmoInativo() throws Exception {
		UUID usuario = dados.usuario("Usuário Filtro Listagem", "ATIVA");
		UUID profissional = dados.colaborador("Profissional Filtro Listagem", true);
		registrar("COM_USUARIOS", LocalDate.of(2005, 6, 1), List.of(usuario), List.of(profissional));
		dados.inativarColaborador(profissional);

		mvc.perform(get(URL + "/filtros"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profissionais[*].id").value(hasItem(profissional.toString())))
			.andExpect(jsonPath("$.usuarios[*].pessoaId").value(hasItem(usuario.toString())));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "listagem-admin")
	void administrativoNaoLista() throws Exception {
		mvc.perform(get(URL).param("pessoaId", UUID.randomUUID().toString())).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/filtros")).andExpect(status().isForbidden());
	}

	private String registrar(String tipo, LocalDate data, List<UUID> participantes, List<UUID> profissionais)
			throws Exception {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("tipo", tipo);
		corpo.put("data", data.toString());
		corpo.put("titulo", "Atividade da listagem");
		corpo.put("descricao", "Texto fictício.");
		corpo.put("participantes", participantes);
		corpo.put("profissionais", profissionais);
		String resposta = mvc
			.perform(post(URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(corpo)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(resposta, "$.id");
	}

}
